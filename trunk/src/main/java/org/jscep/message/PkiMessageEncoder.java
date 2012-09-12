/*
 * Copyright (c) 2010 ThruPoint Ltd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jscep.message;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.jscep.transaction.PkiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to encode a <tt>pkiMessage</tt> into a PKCS #7 signedData
 * object.
 * 
 * @see PkiMessageDecoder
 */
public final class PkiMessageEncoder {
    private static final Logger LOGGER = LoggerFactory
	    .getLogger(PkiMessageEncoder.class);
    private final PrivateKey signerKey;
    private final X509Certificate signerId;
    private final PkcsPkiEnvelopeEncoder enveloper;

    /**
     * Creates a new <tt>PkiMessageEncoder</tt> instance.
     * 
     * @param signerKey
     *            the key to use to sign the <tt>signedData</tt>.
     * @param signerId
     *            the certificate to use to identify the signer.
     * @param enveloper
     *            the enveloper used for encoding the <tt>messageData</tt>
     */
    public PkiMessageEncoder(PrivateKey signerKey, X509Certificate signerId,
	    PkcsPkiEnvelopeEncoder enveloper) {
	this.signerKey = signerKey;
	this.signerId = signerId;
	this.enveloper = enveloper;
    }

    /**
     * Encodes the provided <tt>PkiMessage</tt> into a PKCS #7
     * <tt>signedData</tt>.
     * 
     * @param message
     *            the <tt>PkiMessage</tt> to encode.
     * @return the encoded <tt>signedData</tt>
     * @throws MessageEncodingException
     *             if there is a problem encoding the <tt>PkiMessage</tt>
     */
    public CMSSignedData encode(PkiMessage<?> message)
	    throws MessageEncodingException {
	LOGGER.debug("Encoding pkiMessage");
	LOGGER.debug("Encoding message: {}", message);
	CMSProcessableByteArray signable;

	boolean hasMessageData = true;
	if (message instanceof CertRep) {
	    CertRep response = (CertRep) message;
	    if (response.getPkiStatus() != PkiStatus.SUCCESS) {
		hasMessageData = false;
	    }
	}
	if (hasMessageData) {
	    CMSEnvelopedData ed;
	    if (message.getMessageData() instanceof byte[]) {
		ed = enveloper.encode((byte[]) message.getMessageData());
	    } else if (message.getMessageData() instanceof PKCS10CertificationRequest) {
		try {
		    ed = enveloper.encode(((PKCS10CertificationRequest) message
			    .getMessageData()).getEncoded());
		} catch (IOException e) {
		    throw new MessageEncodingException(e);
		}
	    } else if (message.getMessageData() instanceof CMSSignedData) {
		try {
		    ed = enveloper.encode(((CMSSignedData) message
			    .getMessageData()).getEncoded());
		} catch (IOException e) {
		    throw new MessageEncodingException(e);
		}
	    } else {
		try {
		    ed = enveloper.encode(((ASN1Object) message
			    .getMessageData()).getEncoded());
		} catch (IOException e) {
		    throw new MessageEncodingException(e);
		}
	    }
	    try {
		signable = new CMSProcessableByteArray(ed.getEncoded());
	    } catch (IOException e) {
		throw new MessageEncodingException(e);
	    }
	} else {
	    signable = null;
	}

	CMSSignedDataGenerator sdGenerator = new CMSSignedDataGenerator();
	LOGGER.debug(
		"Signing pkiMessage using key belonging to [issuer={}; serial={}]",
		signerId.getIssuerDN(), signerId.getSerialNumber());
	try {
	    sdGenerator.addSignerInfoGenerator(getSignerInfo(message));
	} catch (CertificateEncodingException e) {
	    throw new MessageEncodingException(e);
	} catch (OperatorCreationException e) {
	    throw new MessageEncodingException(e);
	}
	try {
	    Collection<X509Certificate> certColl = Collections
		    .singleton(signerId);
	    sdGenerator.addCertificates(new JcaCertStore(certColl));
	} catch (CMSException e) {
	    throw new MessageEncodingException(e);
	} catch (CertificateEncodingException e) {
	    throw new MessageEncodingException(e);
	}
	LOGGER.debug("Signing {} content", signable);
	try {
	    CMSSignedData pkiMessage = sdGenerator.generate(
		    "1.2.840.113549.1.7.1", signable, true, (Provider) null,
		    true);
	    LOGGER.debug("Finished encoding pkiMessage");
	    return pkiMessage;
	} catch (Exception e) {
	    throw new MessageEncodingException(e);
	}
    }

    private SignerInfoGenerator getSignerInfo(PkiMessage<?> message)
	    throws OperatorCreationException, CertificateEncodingException {
	JcaSignerInfoGeneratorBuilder signerInfoBuilder = new JcaSignerInfoGeneratorBuilder(
		getDigestCalculator());
	signerInfoBuilder
		.setSignedAttributeGenerator(getTableGenerator(message));
	SignerInfoGenerator signerInfo = signerInfoBuilder.build(
		getContentSigner(), signerId);
	return signerInfo;
    }

    private CMSAttributeTableGenerator getTableGenerator(PkiMessage<?> message) {
	AttributeTableFactory attrFactory = new AttributeTableFactory();
	AttributeTable signedAttrs = attrFactory.fromPkiMessage(message);
	CMSAttributeTableGenerator atGen = new DefaultSignedAttributeTableGenerator(
		signedAttrs);
	return atGen;
    }

    private DigestCalculatorProvider getDigestCalculator()
	    throws OperatorCreationException {
	return new JcaDigestCalculatorProviderBuilder().build();
    }

    private ContentSigner getContentSigner() throws OperatorCreationException {
	JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(
		"SHA1withRSA");
	return contentSignerBuilder.build(signerKey);
    }
}