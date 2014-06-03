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

<h4>Upload a file and some field data:</h4>
This site allows you to test the JBoss EAP Server
<br/>
<br/>
Select a file to upload: 
<form action="upload" method="post" enctype="multipart/form-data">
	<h3>Enter name: </h3><input type="text/ascii" name="filename" size="50" />
	<br/><p/>
	<h3>Select contents: </h3><input type="file" name="file" size="50" />
	<br/><p/>
	<input type="submit" value="Upload File" />
</form>

<form action="upload" method="post" enctype="multipart/form-data">
	<h3>Enter name: </h3><input type="text/ascii" name="filename" size="50" />
	<br/><p/>
	<input type="submit" value="Show File" />
</form>

<form action="upload" method="post" enctype="multipart/form-data">
	<input type="text/ascii" name="readonly" size="50" />
	<br/><p/>
	<input type="submit" value="List All" />
</form>

<form action="upload" method="post" enctype="multipart/form-data">
	<input type="text/ascii" name="delete" size="50" />
	<br/><p/>
	<input type="submit" value="List All" />
</form>

</body>
</html>
