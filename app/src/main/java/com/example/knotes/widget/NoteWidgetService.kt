package com.example.knotes.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.knotes.R
import com.example.knotes.data.KNotesDatabase
import com.example.knotes.data.entity.Note
import kotlinx.coroutines.runBlocking

class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetRemoteViewsFactory(this.applicationContext)
    }
}

class NoteWidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var pinnedNotes: List<Note> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // This is a simplified version. In a real app, use a Repository.
        val db = KNotesDatabase.getDatabase(context)
        pinnedNotes = runBlocking {
            db.noteDao().getPinnedNotesSync() // Need to add this to DAO
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = pinnedNotes.size

    override fun getViewAt(position: Int): RemoteViews {
        val note = pinnedNotes[position]
        val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        views.setTextViewText(android.R.id.text1, note.title)
        views.setTextColor(android.R.id.text1, context.getColor(android.R.color.white))
        
        val fillInIntent = Intent().apply {
            putExtra("noteId", note.id)
        }
        views.setOnClickFillInIntent(android.R.id.text1, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = pinnedNotes[position].id.toLong()
    override fun hasStableIds(): Boolean = true
}
