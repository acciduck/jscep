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

package com.google.code.jscep.request;

import java.util.logging.Logger;

import com.google.code.jscep.content.CaCapabilitiesContentHandler;
import com.google.code.jscep.response.Capabilities;
import com.google.code.jscep.util.LoggingUtil;

/**
 * This class represents a <tt>GetCACaps</tt> request.
 * 
 * @link http://tools.ietf.org/html/draft-nourse-scep-19#appendix-D.1
 */
public class GetCACaps implements Request<Capabilities> {
	private static Logger LOGGER = LoggingUtil.getLogger("com.google.code.jscep.request");
	private String caIdentifier;

	public GetCACaps() {
	}

	public GetCACaps(String caIdentifier) {
		this.caIdentifier = caIdentifier;
	}

	/**
	 * {@inheritDoc}
	 */
	public Operation getOperation() {
		return Operation.GetCACaps;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getMessage() {
		return caIdentifier;
	}

	/**
	 * {@inheritDoc}
	 */
	public CaCapabilitiesContentHandler getContentHandler() {
		return new CaCapabilitiesContentHandler();
	}
}