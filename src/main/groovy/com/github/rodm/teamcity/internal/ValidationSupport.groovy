/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity.internal

import org.xml.sax.SAXNotRecognizedException

import javax.xml.XMLConstants

class ValidationSupport {

    static XmlParser createXmlParser() {
        return createXmlParser(false)
    }

    static XmlParser createXmlParser(boolean offline) {
        def parser = new XmlParser(false, true, true)
        if (offline) {
            parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
        }
        setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_DTD, "file,http")
        setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,http")
        return parser
    }

    private static void setParserProperty(XmlParser parser, String uri, Object value) {
        try {
            parser.setProperty(uri, value)
        }
        catch (SAXNotRecognizedException ignore) { }
    }
}
