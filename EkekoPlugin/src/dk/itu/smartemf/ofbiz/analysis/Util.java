/**
 * Copyright 2008 Anders Hessellund 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id: Util.java,v 1.1 2008/01/17 18:48:19 hessellund Exp $
 */
package dk.itu.smartemf.ofbiz.analysis;
//inspired by source code from a compiler course by Robby
class Util {
	/**
	 * Returns the first line of an object's {@link String} representation
	 * @param o The object.
	 * @return The first line of the given object's {@link String} representation
	 */
	static String getFirstLine(Object o) {
		assert o != null;
		String nText = o.toString();
		int index = nText.indexOf('\n');
		String result = index >= 0 ? nText.substring(0, index) : nText;
		return result;
	}
	
}
