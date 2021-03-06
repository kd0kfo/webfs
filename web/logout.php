<?php

require_once("includes.php");
require_once("auth.php");

$username = get_requested_string("username");
if(!$username) {
	json_exit("No username provided.", 1);
}

$auth = new Auth($username);
$auth->clear_session_key($username);
$auth->remove_upload_token();
json_exit("Logout successful.", 0);
?>
