<?php

require_once("includes.php");
require_once("classes.php");
require_once("auth.php");

$auth = new Auth(get_requested_string("username"));

$source = get_requested_string("source");
$destination = get_requested_string("destination");
if(!$auth->authenticate($source . $destination, get_requested_string("signature"))) {
	json_exit("Invalid authentication", 1);
}

if(strlen($source) == 0 || strlen($destination) == 0) {
	json_exit("Require a source parameter and destination parameter", 1);
}

$source = new WebFile($source);
$destination = new WebFile($destination);

if(!$source->exists())
	json_exit($source->orig_filename . " does not exist.", 1);

$retval = $source->move_to($destination);
if(!$retval) {
	json_exit("Could not move " . $source->filepath . " to " . $destination->filepath, 1);
} else {
	json_exit("Moved " . $source->filepath . " to " . $destination->filepath, 0);
}

?>