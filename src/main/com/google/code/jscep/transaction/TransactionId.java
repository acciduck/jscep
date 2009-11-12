/*
 * Copyright (c) 2009 David Grant
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

package com.google.code.jscep.transaction;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Hex;

/**
 * @see http://tools.ietf.org/html/draft-nourse-scep-19#section-2.3.1
 * @see http://tools.ietf.org/html/draft-nourse-scep-19#section-3.1.1.1
 */
public final class TransactionId {
	private final static Logger LOGGER = Logger.getLogger(TransactionId.class.getName());
	private static final AtomicLong ID_SOURCE = new AtomicLong();
	private final byte[] id;
	
	public TransactionId(byte[] id) {
		this.id = id;
	}
	
	private TransactionId(KeyPair keyPair, String fingerprintAlgorithm) {
		LOGGER.info("Generated new TransactionId from Key Pair");
    	MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(fingerprintAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        id = Hex.encode(digest.digest(keyPair.getPublic().getEncoded()));
	}
	
	private TransactionId() {
		LOGGER.info("Generated new TransactionId");
		id = Long.toHexString(ID_SOURCE.getAndIncrement()).getBytes();
	}
	
	public byte[] getBytes() {
		return id;
	}
	
	@Override
	public boolean equals(Object o) {
		TransactionId transId = (TransactionId) o;
		
		return Arrays.equals(transId.getBytes(), getBytes());
	}
	
	public static TransactionId createTransactionId(KeyPair keyPair, String fingerprintAlgorithm) {
		if (keyPair == null) {
			return new TransactionId();
		} else {
			return new TransactionId(keyPair, fingerprintAlgorithm);
		}
	}
	
	public static TransactionId createTransactionId() {
		return new TransactionId();
	}
}
