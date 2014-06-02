/**
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.my.eaptest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;

import org.apache.log4j.Logger;

import javax.inject.Inject;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@WebServlet(name = "UploadServlet", urlPatterns = {"/upload"})
@MultipartConfig
public class UploadServlet extends HttpServlet {
    private final int MAX_FILE_SIZE = 5000 * 1024;

    @Inject
    private Logger log;

    private static ConcurrentHashMap<String, String> filedata = new ConcurrentHashMap();

    private static String fromStream(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();

        // Check that we have a file upload request.
        if (!ServletFileUpload.isMultipartContent(req)) {
            out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet upload</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<p>No file uploaded!</p>");
            out.println("</body>");
            out.println("</html>");

            return;
        }

        // Create a factory for disk-based file items.
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // Configure a repository (to ensure a secure temp location is used).
        ServletContext servletContext = this.getServletConfig().getServletContext();
        File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        factory.setRepository(repository);
        // Maximum size that will be stored in memory.
        factory.setSizeThreshold(MAX_FILE_SIZE);
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request to get file items.
        try {
            List<FileItem> items = upload.parseRequest(req);

            out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet upload</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div style=\"text-align: center;\">");
            // we look for a filename item and/or file item with file content
            Iterator<FileItem> iter = items.iterator();
            String filename = null;
            String contents = null;
            while (iter.hasNext()) {
                FileItem item = iter.next();
                boolean isFormField = item.isFormField();
                String fieldName = item.getFieldName();
                if (isFormField && fieldName.equals("filename")) {
                    filename = item.getString();
                } else if (!isFormField && fieldName.equals("file")) {
                    contents = fromStream(item.getInputStream());
                } else {
                    if (isFormField) {
                        out.println("<p><pre>Unexpected field value : " + fieldName + "</pre>");
                    } else {
                        String name = item.getName();
                        out.println("<p><pre>Unexpected file value : " + name + " for field " + fieldName + "</pre>");
                    }
                    out.println("</body>");
                    out.println("</html>");
                    return;
                }
            }

            // if we don't have a filename then list all files in the hashtable

            if (filename == null) {
                Set<String> keys = filedata.keySet();
                out.println("Keys:<br/>");
                out.println("<pre>");
                for (String key : keys) {
                    out.println(key);
                }
                out.println("</pre>");
                out.println("</body>");
                out.println("</html>");
                return;
            }
            // if we have a filename and no contents then we
            // retrieve the file contents from the hashmap
            // if we have a filename and contents then we update
            // the hashmap

            if (contents == null) {
                contents = filedata.get(filename);
            } else {
                filedata.put(filename, contents);
            }

            // now hand the contents back
            iter = items.iterator();
            out.println("<pre>File: ");
            out.println(filename);
            out.println("</pre><br/>");
            out.println("<pre>");
            out.println(contents);
            out.println("</pre>");
            out.println("</body>");
            out.println("</html>");
        } catch (FileUploadException fuEx) {
            log.error("Problem with process file upload:", fuEx);
        }
    }
}