/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.examples.util;

import java.io.OutputStream;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.contentstream.operator.Operator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is an example on how to remove all text from PDF document.
 *
 * @author Ben Litchfield
 */
public final class RemoveAllText
{
    /**
     * Default constructor.
     */
    private RemoveAllText()
    {
        // example class should not be instantiated
    }

    /**
     * This will remove all text from a PDF document.
     *
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length != 2)
        {
            usage();
        }
        else
        {
            try (PDDocument document = PDDocument.load(new File(args[0])))
            {
                if (document.isEncrypted())
                {
                    System.err.println(
                            "Error: Encrypted documents are not supported for this example.");
                    System.exit(1);
                }
                for (PDPage page : document.getPages())
                {
                    removeAllTextTokens(page, document);
                }
                document.save(args[1]);
            }
        }
    }

    private static void processResources(PDResources resources) throws IOException
    {
        Iterable<COSName> names = resources.getXObjectNames();
        for (COSName name : names)
        {
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDFormXObject)
            {
                removeAllTextTokens((PDFormXObject) xobject);
            }
        }
    }

    private static void removeAllTextTokens(PDPage page, PDDocument document) throws IOException
    {
        PDFStreamParser parser = new PDFStreamParser(page);
        parser.parse();
        List<Object> tokens = parser.getTokens();
        List<Object> newTokens = new ArrayList<>();
        for (Object token : tokens)
        {
            if (token instanceof Operator)
            {
                String opname = ((Operator) token).getName();
                if ("TJ".equals(opname) || "Tj".equals(opname))
                {
                    // remove the one argument to this operator
                    newTokens.remove(newTokens.size() - 1);
                    continue;
                }
            }
            newTokens.add(token);
        }
        PDStream newContents = new PDStream(document);
        try (OutputStream out = newContents.createOutputStream(COSName.FLATE_DECODE))
        {
            ContentStreamWriter writer = new ContentStreamWriter(out);
            writer.writeTokens(newTokens);
        }
        page.setContents(newContents);
        processResources(page.getResources());
    }

    private static void removeAllTextTokens(PDFormXObject xobject) throws IOException
    {
        PDStream stream = xobject.getContentStream();
        PDFStreamParser parser = new PDFStreamParser(xobject);
        parser.parse();
        List<Object> tokens = parser.getTokens();
        List<Object> newTokens = new ArrayList<>();
        for (Object token : tokens)
        {
            if (token instanceof Operator)
            {
                Operator op = (Operator) token;
                if ("TJ".equals(op.getName()) || "Tj".equals(op.getName()) ||
                     "'".equals(op.getName()) || "\"".equals(op.getName()))
                {
                    // remove the one argument to this operator
                    newTokens.remove(newTokens.size() - 1);
                    continue;
                }
            }
            newTokens.add(token);
        }
        try (OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE))
        {
            ContentStreamWriter writer = new ContentStreamWriter(out);
            writer.writeTokens(newTokens);
        }
        processResources(xobject.getResources());
    }

    /**
     * This will print the usage for this document.
     */
    private static void usage()
    {
        System.err.println(
                "Usage: java " + RemoveAllText.class.getName() + " <input-pdf> <output-pdf>");
    }

}
