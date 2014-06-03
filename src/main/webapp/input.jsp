<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<title>File Uploading Form</title>
</head>
<body>

<div style="text-align: center;">
	<h1>Welcome to Andrew's eap test!</h1>
</div>

<hr>                    

This site allows you to test the JBoss EAP Server
<br/>
<br/>
<h4>Upload a named file:</h4>
Select a file to upload:
<form action="upload" method="post" enctype="multipart/form-data">
	<h3>Select file: </h3><input type="file" name="file" size="50" />
	<br/><p/>
	<h3>Enter alternative name (optional): </h3><input type="text/ascii" name="filename" size="50" />
	<br/><p/>
	<input type="submit" value="Upload File" />
</form>

<hr>

<h4>Show a named file:</h4>
<form action="upload" method="post" enctype="multipart/form-data">
	<h3>Enter name: </h3><input type="text/ascii" name="filename" size="50" />
	<br/><p/>
	<input type="submit" value="Show File" />
</form>

<hr>

<h4>Delete a named file:</h4>
<form action="upload" method="post" enctype="multipart/form-data">
	<h3>Enter name: </h3><input type="text/ascii" name="filename" size="50" />
    <input type="hidden" name="delete" value="" />
	<br/><p/>
	<input type="submit" value="Delete File" />
</form>

<hr>

<h4>List all uploaded files:</h4>
<form action="upload" method="post" enctype="multipart/form-data">
	<input type="hidden" name="readonly" value="" />
	<br/><p/>
	<input type="submit" value="List All" />
</form>

<hr>

<h4>Delete all uploaded files:</h4>
<form action="upload" method="post" enctype="multipart/form-data">
	<input type="hidden" name="delete" value="" />
	<br/><p/>
	<input type="submit" value="Delete All" />
</form>

</body>
</html>
