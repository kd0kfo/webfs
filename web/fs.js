var webfs = webfs || {};
webfs.error = {
	SUCCESS : 0,
	CLIENT_ERROR : 1,
	DEBUG_BREAK : 2,
	ACCESS_DENIED : 3,
	DB_ERROR : 4
};

function loadPageVar (sVar) {
  return decodeURI(window.location.search.replace(new RegExp("^(?:.*[&\\?]" + encodeURI(sVar).replace(/[\.\+\*]/g, "\\$&") + "(?:\\=([^&]*))?)?.*$", "i"), "$1"));
}

function basename(path) {
	return path.split(/[\\/]/).pop();
}

function hoverin() {
    $(this).css("font-style", "italic").css("color", "red");
}

function hoverout() {
    $(this).css("font-style", "normal").css("color", "black");
}

function get_server_info() {
	var server_info = $.ajax({ 
        url: 'info.php',
        async: false
     }).responseText;
  return $.parseJSON(server_info);
}

function display_json_message(data) {
	var json = jQuery.parseJSON(data);
	$("#result").empty().append(json.message);
}
	
function json2ul(json) {
	var ul = $('<ul>');
	$.each(json, function(key, val) {
		ul.append($('<li>').text(key + ": " + val));
	});
	return ul;		
}

function updateDir(thedirname, username, sessionkey) {
    window.document.title = thedirname;
    $("#heading").text("Contents of " + thedirname);
    var signature = sign_data(thedirname);
    var result = $.post("ls.php", {"filename": thedirname, "username": username, "signature": signature,"debugtime": thedirname + unixtime().toString()});
    result.done(function(responsetext) {
	    var data = $.parseJSON(responsetext);
	    $("#content").text("");
	    if(data['status'] && data['status'] != 0) {
	    	$("#content").text("ERROR: " + data['message']);
	    }
	    else if(data['type'] == "f") {
	    	$("#content").text("File: " + data['name']);
	    }
	    else if(data['type'] == "d") {
			if(thedirname != "/") {
			    var parentdir = $("<p/>", {class: "d"});
			    parentdir.text("Up to " + data["parent"]);
			    parentdir.click(function() {updateDir(data["parent"], username, sessionkey);});
			    parentdir.hover(hoverin,
					    hoverout);
			    $("#content").append(parentdir);
			}
			var debug = $("#debug");
			$.each(data['dirents'], function(key, val) {
				if(val["name"] == thedirname) {
				    return 1;
				}
				var text = thedirname;
				if(thedirname != "/") {
				    text = text + "/";
				}
				text = text + val["name"];
				var dirent = $("<p/>", {class: val["type"]});
				dirent.text(text);
				if(val["type"] == "d") {
				    dirent.click(function() {updateDir($(this).text().trim(), username, sessionkey);});
				} else {
				    dirent.click(function() {window.location="file.html?filename=" + text;});
				}
				dirent.hover(hoverin, hoverout);
				$("#content").append(dirent);
			});
	    }
	});
	result.fail(function(jqXHR){
		var json = $.parseJSON(jqXHR.responseText);
		if(json['status'] == webfs.error.ACCESS_DENIED) {
			window.location.assign("logon.html");
		}
		$("#content").text("Error: " + json['message']);
	});
}

function unixtime() {
	return Math.round(+new Date()/1000);
}

function hash(data, key) {
	var hash = CryptoJS.HmacSHA256(data, key);
	return CryptoJS.enc.Base64.stringify(hash);
}

function sign_data(data) {
	if(!localStorage['sessionkey'])
		throw "Login required";
	var timestamp = unixtime();
	if(localStorage['offset']) {
		timestamp = timestamp + localStorage['offset']*1;
	}
	return hash(data + timestamp.toString(), localStorage['sessionkey']);
}	
