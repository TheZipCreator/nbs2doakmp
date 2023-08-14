package nbs2doakmp;

import androidx.compose.foundation.layout.*;
import androidx.compose.material.*;
import androidx.compose.runtime.*;
import androidx.compose.runtime.snapshots.*;
import androidx.compose.ui.*;
import androidx.compose.ui.unit.*;
import androidx.compose.ui.window.*;

import java.io.*;
import java.nio.*;

fun main() = application {
	Window(onCloseRequest = ::exitApplication, title = "nbs2doakmp", state = rememberWindowState(width = 400.dp, height = 600.dp)) {
		var input by remember { mutableStateOf("") };
		var output by remember { mutableStateOf("") };
		var status by remember { mutableStateOf("") };
		var comments by remember { mutableStateOf(false) };
		MaterialTheme {
			Column(Modifier.fillMaxSize().size(width = 400.dp, height = 400.dp), Arrangement.SpaceAround) {
				val textMod = Modifier.align(Alignment.CenterHorizontally).size(width = 350.dp, height = 50.dp);
				TextField(value = input, onValueChange = { input = it }, label = { Text("Input NBS file") }, modifier = textMod);
				TextField(value = output, onValueChange = { output = it }, label = { Text("Output") }, 
					modifier = Modifier.align(Alignment.CenterHorizontally).size(width = 350.dp, height = 350.dp), readOnly = true);
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
					Checkbox(checked = comments, onCheckedChange = { comments = it })
					Text("Output with comments.", modifier = Modifier.align(Alignment.CenterVertically));
				}
				Button(onClick = {
					try {
						status = "Converting..."
						val song = Song.load(ByteBuffer.wrap(File(input).readBytes()));
						output = song.toDoakMP(comments);
						status = "Converted sucessfully.";
					} catch(e: ParseException) {
						status = "Parsing Error: ${e.message}" ?: "Parsing error.";
						e.printStackTrace();
					} catch(e: IOException) {
						status = e.message ?: "IO error.";
						e.printStackTrace();
					} catch(e: Exception) {
						status = "Error: $e";
						e.printStackTrace();
					}
				}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
					Text("Convert");
				}
				Text(status, modifier = Modifier.align(Alignment.CenterHorizontally))
			}
		}
	}
}
