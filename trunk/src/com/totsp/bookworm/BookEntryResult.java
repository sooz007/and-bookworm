package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.totsp.bookworm.data.IBookDataSource;
import com.totsp.bookworm.model.Author;
import com.totsp.bookworm.model.Book;
import com.totsp.bookworm.util.CoverImageUtil;

public class BookEntryResult extends Activity {

   public static final String FROM_RESULT = "FROM_RESULT";

   private BookWormApplication application;

   // package scope for use in inner class (Android optimization)
   Button bookAddButton;
   TextView bookTitle;
   ImageView bookCover;
   TextView bookAuthors;

   Book book;

   boolean fromSearch;

   private GetBookDataTask getBookDataTask;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.bookentryresult);
      this.application = (BookWormApplication) this.getApplication();

      this.getBookDataTask = null;

      this.bookTitle = (TextView) this.findViewById(R.id.bookentrytitle);
      this.bookCover = (ImageView) this.findViewById(R.id.bookentrycover);
      this.bookAuthors = (TextView) this.findViewById(R.id.bookentryauthors);

      this.bookAddButton = (Button) this.findViewById(R.id.bookentryaddbutton);
      this.bookAddButton.setVisibility(View.INVISIBLE);
      this.bookAddButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            BookEntryResult.this.bookAddClick();
         }
      });

      // several other activites can populate this one
      // ISBN must be present as intent extra to proceed
      String isbn = this.getIntent().getStringExtra(Constants.ISBN);
      if ((isbn == null) || (isbn.length() < 10) || (isbn.length() > 13)) {
         Log.e(Constants.LOG_TAG, "Invalid ISBN passed to BookEntryResult - " + isbn);
         this.setViewsForInvalidEntry(isbn);
      } else {
         this.getBookDataTask = new GetBookDataTask();
         this.getBookDataTask.execute(isbn);
      }

      this.fromSearch = this.getIntent().getBooleanExtra(BookEntrySearch.FROM_SEARCH, false);
   }

   @Override
   public void onPause() {
      if ((this.getBookDataTask != null) && this.getBookDataTask.dialog.isShowing()) {
         this.getBookDataTask.dialog.dismiss();
      }
      super.onPause();
   }

   private void bookAddClick() {
      if ((this.book != null) && (this.book.isbn10 != null)) {
         // TODO check for book exists using more than just ISBN or title (these are not unique - use a combination maybe?)
         // save book to database
         long bookId = this.application.getDataHelper().insertBook(this.book);
         if (this.book.coverImage != null) {
            BookEntryResult.this.application.getDataImageHelper().storeBitmap(this.book.coverImage, this.book.title,
                     bookId);
         }
      }
      if (this.fromSearch) {
         // if from search results, return to search
         Intent intent = new Intent(BookEntryResult.this, BookEntrySearch.class);
         intent.putExtra(BookEntryResult.FROM_RESULT, true);
         this.startActivity(intent);
      } else {
         this.startActivity(new Intent(BookEntryResult.this, Main.class));
      }
   }

   private void setViewsForInvalidEntry(final String isbn) {
      this.bookCover.setImageResource(R.drawable.book_invalid_isbn);
      if (isbn != null) {
         this.bookAuthors.setText("Whoops, that entry didn't work (ISBN: " + isbn + " was not resolved). "
                  + "Please try either another entry method, or another title.");
      } else {
         this.bookAuthors.setText("Whoops, that entry didn't work (ISBN was not resolved). "
                  + "Please try either another entry method, or another title.");
      }
   }

   //
   // AsyncTasks
   //
   private class GetBookDataTask extends AsyncTask<String, Void, Book> {
      private final ProgressDialog dialog = new ProgressDialog(BookEntryResult.this);

      private String coverImageProviderKey;

      protected void onPreExecute() {
         this.dialog.setMessage("Retrieving book data..");
         this.dialog.show();
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BookEntryResult.this);
         // default to OpenLibrary(2) for cover image provider - for now (doesn't require login)
         this.coverImageProviderKey = prefs.getString("coverimagelistpref", "2");
      }

      protected Book doInBackground(final String... isbns) {
         if (isbns[0] != null) {
            IBookDataSource dataSource = BookEntryResult.this.application.getBookDataSource();
            if (dataSource != null) {
               Book b = dataSource.getBook(isbns[0]);
               if (b == null) {
                  Log.e(Constants.LOG_TAG, "BookWorm GetBookDataTask book returned from data source (using ISBN - "
                           + isbns[0] + ") null, cannot add book.");
                  return null;
               }
               if (b.isbn10 != null) {
                  Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(this.coverImageProviderKey, b.isbn10);
                  b.coverImage = (coverImageBitmap);
               } else if (b.isbn13 != null) {
                  Bitmap coverImageBitmap = CoverImageUtil.retrieveCoverImage(this.coverImageProviderKey, b.isbn13);
                  b.coverImage = (coverImageBitmap);
               }
               return b;
            } else {
               Log.e(Constants.LOG_TAG, "BookWorm GetBookDataTask book data source null, cannot add book.");
            }
         } else {
            Log.e(Constants.LOG_TAG, "BookWorm GetBookDataTask ISBN null, cannot add book.");
         }
         return null;
      }

      protected void onPostExecute(final Book b) {
         if (this.dialog.isShowing()) {
            this.dialog.dismiss();
         }

         if (b != null) {
            BookEntryResult.this.bookTitle.setText(b.title);
            String authors = null;
            for (Author a : b.authors) {
               if (authors == null) {
                  authors = a.name;
               } else {
                  authors += ", " + a.name;
               }
            }
            BookEntryResult.this.bookAuthors.setText(authors);

            if (b.coverImage != null) {
               if (BookEntryResult.this.application.isDebugEnabled()) {
                  Log.d(Constants.LOG_TAG, "book cover bitmap present, set cover");
               }
               BookEntryResult.this.bookCover.setImageBitmap(b.coverImage);
            } else {
               if (BookEntryResult.this.application.isDebugEnabled()) {
                  Log.d(Constants.LOG_TAG, "book cover not found, generate image");
               }
               Bitmap generatedCover = BookEntryResult.this.application.getDataImageHelper().createCoverImage(b.title);
               BookEntryResult.this.bookCover.setImageBitmap(generatedCover);
               b.coverImage = generatedCover;
            }

            BookEntryResult.this.book = b;
            BookEntryResult.this.bookAddButton.setVisibility(View.VISIBLE);
         } else {
            BookEntryResult.this.setViewsForInvalidEntry(null);
         }
      }
   }
}