/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.util;

import eye.Comm.NameSpace;
import eye.Comm.NameValueSet;
import eye.Comm.NameValueSet.NodeType;

public class PrintNode {
	public static void print(NameValueSet nvs) {
		printNVS(nvs, 0);
	}

	public static void printNameSpace(NameSpace ns) {
		if (ns == null)
			return;

		System.out.println("NameSpace: ");
		System.out.println("  ID : " + ns.getUserId());
		System.out.println("  Name: " + ns.getName());
		System.out.println("  City: " + ns.getCity());
		System.out.println("  Zip Code: " + ns.getZipCode());
		System.out.print("");
	}

	private static void printNVS(NameValueSet nvs, int level) {
		if (nvs == null)
			return;

		String indent = "";
		for (int n = 0; n < level; n++)
			indent += "  ";

		if (nvs.getNodeType() == NodeType.VALUE) {
			System.out.println(indent + nvs.getName() + " = " + nvs.getValue());
		} else {
			if (nvs.hasName())
				System.out.println(indent + nvs.getName() + "[");
			else
				System.out.println(indent + "[");

			if (nvs.getNodeCount() > 0) {
				for (int i = 0, I = nvs.getNodeCount(); i < I; i++) {
					NameValueSet child = nvs.getNode(i);
					if (child.getNodeCount() > 0) {
						for (int j = 0, J = child.getNodeCount(); j < J; j++) {
							printNVS(child, level + 1);
						}
					}
				}
			}

			System.out.println(indent + "]");
		}
	}
}
