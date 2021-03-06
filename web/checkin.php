<?php 

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");
require_once("includes/webfile.php");

$username = get_requested_string("username");
$auth = new Auth($username);

$filename = get_requested_filename();
if(!$filename) {
	json_exit("Missing filename", 1);
}
if(!$auth->authenticate($filename, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

$file = new WebFile($filename);

if(!$file->exists()) {
	json_exit("File not found", 1);
}

$metadata = $file->get_metadata();
$file->checkin($username);
json_exit("Checked in $filename", 0);

?>
