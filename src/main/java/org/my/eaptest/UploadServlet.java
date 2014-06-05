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
import java.util.ArrayList;
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

    /**
     * auxiliary class which holds last N occurences of a given file
     * ensuring that we keep hold of file contents for a reasonable
     * amount of time and thereby forcing more stuff into the old gen
     */
    private static class FileSet
    {
        final public static int MAX_FILES=10;
        ArrayList<String> files;
        FileSet()
        {
            files = new ArrayList<String>(MAX_FILES);
        }
        public String add(String file)
        {
            synchronized (files) {
                if (files.size() == MAX_FILES) {
                    files.remove(MAX_FILES - 1);
                }
                files.add(0, file);
            }
            return file;
        }
        public String get()
        {
            synchronized (files) {
                if (files.size() == 0) {
                    return null;
                } else {
                    return files.get(0);
                }
            }
        }
        public int size()
        {
            return files.size();
        }
    }
    private static ConcurrentHashMap<String, FileSet> filedata = new ConcurrentHashMap();

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
            out.println("<div style=\"text-align: left;\">");
            // we look for a filename item and/or file item with file content
            Iterator<FileItem> iter = items.iterator();
            String filename = null;
            String contents = null;
            boolean delete = false;
            while (iter.hasNext()) {
                FileItem item = iter.next();
                boolean isFormField = item.isFormField();
                String fieldName = item.getFieldName();
                if (isFormField && fieldName.equals("filename")) {
                    String name = item.getString();
                    if (name.length() > 0 || filename == null) {
                        filename = name;
                    }
                } else if (isFormField && fieldName.equals("readonly")) {
                    // means no filename or file provided
                } else if (isFormField && fieldName.equals("delete")) {
                    // delete one or all fiels depending on whether filename is provided
                    delete = true;
                } else if (!isFormField && fieldName.equals("file")) {
                    contents = fromStream(item.getInputStream());
                    contents = StringEscapeUtils.escapeHtml(contents);
                    if (filename == null || filename.length() == 0) {
                        filename = item.getName();
                    }
                } else {
                    if (isFormField) {
                        out.print("<p><pre>Unexpected field name : ");
                        out.print(fieldName);
                        out.println("</pre>");
                    } else {
                        String name = item.getName();
                        out.print("<p><pre>Unexpected file item : ");
                        out.print(name);
                        out.print(" for field ");
                        out.print(fieldName);
                        out.println("</pre>");
                    }
                    out.println("</body>");
                    out.println("</html>");
                    return;
                }
            }

            // if we don't have a filename then either list or delete all files in the hashtable

            if (filename == null) {
                if (delete) {
                    filedata.clear();
                    out.println("All files deleted!<br/>");
                } else {
                    Set<String> keys = filedata.keySet();
                    out.println("All files:<br/>");
                    out.println("<pre>");
                    if (keys.isEmpty()) {
                        out.println("No files found!");
                    } else {
                        for (String key : keys) {
                            out.print(key);
                            FileSet set = filedata.get(key);
                            if (set != null) {
                                out.print(" ");
                                out.println(set.size());
                            } else {
                                out.println(" 0");
                            }
                        }
                    }
                    out.println("</pre>");
                }
                out.println("</body>");
                out.println("</html>");
                return;
            }
            // if we have a filename and no contents then we
            // retrieve the file contents from the hashmap
            // and maybe delete the file
            // if we have a filename and contents then we update
            // the hashmap -- delete should not be supplied in
            // this case

            boolean noUpdate = (contents == null);
            if (noUpdate) {
                if (delete) {
                    FileSet set = filedata.remove(filename);
                    contents = (set != null ? set.get() : null);
                } else {
                    FileSet set = filedata.get(filename);
                    contents = (set != null ? set.get() : null);
                }
            } else {
                FileSet set = new FileSet();
                FileSet old = filedata.putIfAbsent(filename, set);
                if (old != null) {
                    set = old;
                }
                set.add(contents);
            }

            // now hand the contents back
            out.print("<pre>File: ");
            out.print(filename);
            boolean printContents = true;
            if (noUpdate) {
                if (contents == null) {
                    out.println(" not found");
                    printContents = false;
                } else if (delete) {
                    out.print(" deleted");
                }
            } else {
                if (contents == null) {
                    out.print(" added");
                } else {
                    out.print(" updated");
                }
            }
            out.println("</pre><br/>");
            if (printContents) {
                out.println("<pre>");
                out.println(contents);
                out.println("</pre>");
            }
            out.println("</body>");
            out.println("</html>");
        } catch (FileUploadException fuEx) {
            log.error("Problem with process file upload:", fuEx);
        }
    }
}