package nbs2doakmp;

import java.nio.*;

class ParseException(msg: String) : Exception(msg);

private val INSTRUMENTS = listOf("z", "b", "d", "s", "h", "5", "f", "g", "c", "x", "i", "k", "p", "t", "j", "n");
private val KEYS = listOf("a", "A", "b", "c", "C", "d", "D", "e", "f", "F", "g", "G");

data class Note(
	val wait: Short,
	val instrument: Byte,
	val key: Byte,
	val velocity: Byte,
	val panning: UByte,
	val pitch: Short
) {
	companion object {
		fun load(buf: ByteBuffer): List<Note> {
			val notes = mutableListOf<Note>();
			var tickJump = buf.getShort();
			if(tickJump.toInt() == 0)
				return notes;
			var layer = -1;
			while(true) {
				val jumpLayers = buf.getShort();
				if(jumpLayers.toInt() == 0)
					break;
				layer += jumpLayers;
				val instrument = buf.get();
				val key = buf.get();
				val velocity = buf.get();
				val panning = buf.get().toUByte();
				val pitch = buf.getShort();
				if(instrument >= INSTRUMENTS.size)
					throw ParseException("Custom instruments not allowed.");
				notes.add(Note(tickJump, instrument, key, velocity, panning, pitch));
				tickJump = 0;
			}
			return notes;
		}
	}

	fun toDoakMP(song: Song, comments: Boolean) = buildString {
		// instrument
		append(INSTRUMENTS[instrument.toInt()]);
		// pitch
		val k = key+12;
		append(KEYS[k.toInt()%12]);
		// octave
		append(((k-3).toInt()/12)%10);
		// wait
		var amt = (wait*(2000.0/song.songTempo)).toInt();
		if(amt != 0) {
			if(comments)
				append('(');
			while(true) {
				if(amt < 10) {
					append(amt);
					break;
				}
				append('9');
				amt -= 9;
			}
			if(comments)
				append(')');
		}
		if(comments)
			append('.');
	}
}

/// A parsed NBS song
data class Song(
	// header
	val version: Byte,
	val instrumentCount: Byte,
	val songLength: Short,
	val layerCount: Short,
	val songName: String,
	val songAuthor: String,
	val songOriginalAuthor: String,
	val songDescription: String,
	val songTempo: Short,
	val autoSaving: Byte,
	val autoSavingDuration: Byte,
	val timeSignature: Byte,
	val minutesSpent: Int,
	val leftClicks: Int,
	val rightClicks: Int,
	val noteBlocksAdded: Int,
	val noteBlocksRemoved: Int,
	val midiFilename: String,
	val loop: Boolean,
	val loopCount: Byte,
	val loopStartTick: Short,
	val notes: List<Note>
) {
	companion object {
		/// Load from ByteBuffer
		@Throws(ParseException::class) fun load(buf: ByteBuffer): Song {
			buf.order(ByteOrder.LITTLE_ENDIAN);
			try {
				val marker = buf.getShort();
				if(marker.toInt() != 0)
					throw ParseException("Invalid file! (Try opening it with OpenNBS and saving it again.)");
				return Song(
					buf.get(),
					buf.get(),
					buf.getShort(),
					buf.getShort(),
					buf.getString(),
					buf.getString(),
					buf.getString(),
					buf.getString(),
					buf.getShort(),
					buf.get(),
					buf.get(),
					buf.get(),
					buf.getInt(),
					buf.getInt(),
					buf.getInt(),
					buf.getInt(),
					buf.getInt(),
					buf.getString(),
					buf.getBoolean(),
					buf.get(),
					buf.getShort(),
					run {
						val notes = mutableListOf<Note>();
						while(true) {
							val arr = Note.load(buf);
							if(arr.size == 0)
								break;
							notes.addAll(arr);
						}
						// shift every note's wait back one (this is the format doakmp uses)
						for(i in 0..<notes.size) {
							if(i == notes.size-1)
								notes[i] = notes[i].copy(wait = 0);
							else
								notes[i] = notes[i].copy(wait = notes[i+1].wait);
						}
						notes
					}
				);
			} catch(e: BufferUnderflowException) {
				throw ParseException("Unexpected EOF.");
			}
		}
	}

	fun toDoakMP(comments: Boolean): String {
		val song = this;
		return buildString {
			for(n in notes)
				append(n.toDoakMP(song, comments));
			append('#');
		}
	}
}

fun ByteBuffer.getString(): String {
	val length = getInt();
	return buildString {
		for(i in 1..length) {
			append(get().toInt().toChar()); // don't know why toChar() on a byte is deprecated but it is
		}
	}
}

fun ByteBuffer.getBoolean(): Boolean {
	return get() != 0.toByte();
}
