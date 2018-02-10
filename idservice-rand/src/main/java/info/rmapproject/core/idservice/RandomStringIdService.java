/*******************************************************************************
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This software was produced as part of the RMap Project (http://rmap-project.info),
 * The RMap Project was funded by the Alfred P. Sloan Foundation and is a 
 * collaboration between Data Conservancy, Portico, and IEEE.
 *******************************************************************************/
/**
 * 
 */
package info.rmapproject.core.idservice;

import java.net.URI;


/**
 * This is a random number generator that generates random RMap IDs for testing
 * THIS SHOULD NOT BE USED IN PRODUCTION!
 *
 * @author khanson, smorrissey
 */
public class RandomStringIdService implements IdService {

	/**Length of Random String to use for ID**/
	private static final int RANDOM_STRING_LENGTH = 10;
		
	/** The ID prefix. */
	private String idPrefix = "rmap:";

	/** Length of ID to validate against. */
	private int idLength = -1;

	/** String regex to validate an ID against. */
	private String idRegex = "";
	
	
	/**
	 * Instantiates a new Random Number id service.
	 */
	public RandomStringIdService() {

	}

	/* (non-Javadoc)
	 * @see info.rmapproject.core.idservice.IdService#createId()
	 */
	public URI createId() throws Exception {
		URI uri = null;
		//String id = RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH).toLowerCase();
		String id = idPrefix + RandomStringGenerator.generateRandomString(RANDOM_STRING_LENGTH);
		if (isValidId(id)){
			uri= new URI(id);			
		} else {
			throw new Exception("ID failed validation test.  CreateId() failed.");
		}
		return uri;
	}

	/* (non-Javadoc)
	 * @see info.rmapproject.core.idservice.IdService#isValidId(java.net.URI)
	 */
	@Override
	public boolean isValidId(URI id) throws Exception {
		boolean isValid = isValidId(id.toASCIIString());
		return isValid;
	}
		
	/**
	 * Check the string value of an ID is valid by checking it matches a regex and is the right length
	 *
	 * @param id the id
	 * @return boolean
	 * @throws Exception the exception
	 */
	private boolean isValidId(String id) throws Exception {
		boolean isValid = true;
		if (idRegex!=null && idRegex.length()>0){
			isValid = id.matches(idRegex);
		}
		if (isValid && idLength>0) {
			isValid = (id.length()==idLength);
		}
		return isValid;
	}

	/**
	 * Prefixed added to identifiers returned by this service.  Can be configured using the {@code idservice.idPrefix}
	 * property.  Example values:
	 * <ul>
	 *     <li>rmap:</li>
	 *     <li>ark:/12345/</li>
	 * </ul>
	 * <p>
	 * If you are using an ARK ID service, for example, this would be {@code ark:/} followed by the Name Assigning
	 * Authority Number (NAAN) e.g. "ark:/12345/".  For ARK, see http://www.cdlib.org/uc3/naan_table.html for a
	 * registry of NAAN.
	 * </p>
	 *
	 * @return the prefix added to generated identifiers, may be empty or {@code null}
	 */
	public String getIdPrefix() {
		return idPrefix;
	}

	/**
	 * Prefixed added to identifiers returned by this service.  Can be configured using the {@code idservice.idPrefix}
	 * property.  Example values:
	 * <ul>
	 *     <li>rmap:</li>
	 *     <li>ark:/12345/</li>
	 * </ul>
	 * <p>
	 * If you are using an ARK ID service, for example, this would be {@code ark:/} followed by the Name Assigning
	 * Authority Number (NAAN) e.g. "ark:/12345/".  For ARK, see http://www.cdlib.org/uc3/naan_table.html for a
	 * registry of NAAN.
	 * </p>
	 *
	 * @param idPrefix the prefix added to generated identifiers, may be empty or {@code null}
	 */
	public void setIdPrefix(String idPrefix) {
		this.idPrefix = idPrefix;
	}

	/**
	 * The expected length in characters of identifiers generated by this service, after any {@link #getIdPrefix()
	 * prefix} operations have been performed. This class uses the expected length of the identifier to verify that it
	 * is viable.  Can be configured using the {@code idservice.idLength} property.
	 *
	 * @return the expected length of an identifier, or an integer less than 1 if there is no expectation of a
	 *         consistent length
	 */
	public int getIdLength() {
		return idLength;
	}

	/**
	 * The expected length in characters of identifiers generated by this service, after any {@link #getIdPrefix()
	 * prefix} operations have been performed. This class uses the expected length of the identifier to verify that it
	 * is viable.  Can be configured using the {@code idservice.idLength} property.
	 *
	 * @param idLength the expected length of an identifier, or an integer less than 1 if there is no expectation of a
	 *                 consistent length
	 */
	public void setIdLength(int idLength) {
		this.idLength = idLength;
	}

	/**
	 * A regular expression used to match identifiers generated by this service after any {@link
	 * #getIdPrefix() prefix} operations have been performed. This class uses the matching regex to verify that the
	 * identifier is viable.  Can be configured using the {@code idservice.idRegex} property.
	 *
	 * @return the regex used to match the identifier, may be empty or {@code null}
	 */
	public String getIdRegex() {
		return idRegex;
	}

	/**
	 * A regular expression used to match identifiers generated by this service after any {@link
	 * #getIdPrefix() prefix} operations have been performed. This class uses the matching regex to verify that the
	 * identifier is viable.  Can be configured using the {@code idservice.idRegex} property.
	 *
	 * @param idRegex the regex used to match the identifier, may be empty or {@code null}
	 */
	public void setIdRegex(String idRegex) {
		this.idRegex = idRegex;
	}
}