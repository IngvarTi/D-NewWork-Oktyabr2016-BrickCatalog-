package com.tibro.brickcatalog;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.CircularProgressButton;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by Оксана on 13.02.2015.
 */
public class PDFTools {
    private static final String GOOGLE_DRIVE_PDF_READER_PREFIX = "http://drive.google.com/viewer?url=";
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String HTML_MIME_TYPE = "text/html";
    private static final int a = 0;
    private static final int b = 1;

    static SharedPreferences sPref;


    /**
     * If a PDF reader is installed, download the PDF file and open it in a reader.
     * Otherwise ask the user if he/she wants to view it in the Google Drive online PDF reader.<br />
     * <br />
     * <b>BEWARE:</b> This method
     * @param context
     * @param pdfUrl
     * @return
     */
    public static void showPDFUrl( final Context context, final String pdfUrl, CircularProgressButton buttonOpen, CircularProgressButton buttonDelete ) {
        if ( isPDFSupported( context ) ) {
                if(isNetworkStatusAvialable ( context )) {
                    downloadAndOpenPDF(context, pdfUrl, buttonOpen, buttonDelete);
                } else {
                    Toast toast = Toast.makeText(context.getApplicationContext(), context.getString(R.string.noInternet), Toast.LENGTH_SHORT);
 //                   toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();
                    opendPDF(context, pdfUrl);
                }

            } else {
            askToOpenPDFThroughGoogleDrive( context, pdfUrl );
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void opendPDF(final Context context, final String pdfUrl) {
        // Get filename
        final String filename = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        // The place where the downloaded PDF file will be put
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        if (tempFile.exists()) {
            // If we have downloaded the file before, just go ahead and show it.
            openPDF(context, Uri.fromFile(tempFile));
            return;
        }
    }

    /**
     * Downloads a PDF with the Android DownloadManager and opens it with an installed PDF reader app.
     * @param context
     * @param pdfUrl
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void downloadAndOpenPDF(final Context context, final String pdfUrl, final CircularProgressButton buttonOpen,
                                          final CircularProgressButton buttonDelete ) {
        // Get filename
        final String filename = pdfUrl.substring( pdfUrl.lastIndexOf( "/" ) + 1 );
        // The place where the downloaded PDF file will be put
        final File tempFile = new File( context.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS ), filename );
        if ( tempFile.exists() ) {
            // If we have downloaded the file before, just go ahead and show it.
            openPDF( context, Uri.fromFile(tempFile) );
            return;
        }

        buttonOpen.setIndeterminateProgressMode(true);

        // Show progress dialog while downloading
//        final ProgressDialog progress = ProgressDialog.show(context, context.getString(R.string.pdf_show_local_progress_title), context.getString(R.string.pdf_show_local_progress_content), true);

        if (buttonOpen.getProgress() == 0) {
            buttonOpen.setProgress(50);
//            return;
        }
//        if (buttonOpen.getProgress() == 100) {
//            buttonOpen.setProgress(0);
//            return;
//        }

        Toast toast = Toast.makeText(context.getApplicationContext(), context.getString(R.string.startDownloaded), Toast.LENGTH_SHORT);
        toast.show();
        // Create the download request
        DownloadManager.Request r = new DownloadManager.Request( Uri.parse( pdfUrl ) );
        r.setDestinationInExternalFilesDir( context, Environment.DIRECTORY_DOWNLOADS, filename );
        final DownloadManager dm = (DownloadManager) context.getSystemService( Context.DOWNLOAD_SERVICE );
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                if ( !progress.isShowing() ) {
//                    return;
//                }
                context.unregisterReceiver(this);

//                progress.dismiss();
                long downloadId = intent.getLongExtra( DownloadManager.EXTRA_DOWNLOAD_ID, -1 );
                Cursor c = dm.query( new DownloadManager.Query().setFilterById( downloadId ) );

                if ( c.moveToFirst() ) {
                    int status = c.getInt( c.getColumnIndex( DownloadManager.COLUMN_STATUS ) );
                    if ( status == DownloadManager.STATUS_SUCCESSFUL ) {
 //                       openPDF( context, Uri.fromFile( tempFile ) );
                        Toast toast = Toast.makeText(context.getApplicationContext(), context.getString(R.string.fileDownloaded), Toast.LENGTH_SHORT);
                        //          toast.setGravity(Gravity.TOP, 0, 0);
                        toast.show();
                        buttonOpen.setProgress(100);
                        buttonDelete.setVisibility(View.VISIBLE);
                    }
                }
                c.close();
            }
        };
        context.registerReceiver( onComplete, new IntentFilter( DownloadManager.ACTION_DOWNLOAD_COMPLETE ) );

        // Enqueue the request
        dm.enqueue( r );
    }

    /**
     * Show a dialog asking the user if he wants to open the PDF through Google Drive
     * @param context
     * @param pdfUrl
     */
    public static void askToOpenPDFThroughGoogleDrive( final Context context, final String pdfUrl ) {
//        new AlertDialog.Builder( context )
//                .setTitle( R.string.pdf_show_online_dialog_title )
//                .setMessage( R.string.pdf_show_online_dialog_question )
//                .setNegativeButton( R.string.pdf_show_online_dialog_button_no, null )
//                .setPositiveButton( R.string.pdf_show_online_dialog_button_yes, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        openPDFThroughGoogleDrive(context, pdfUrl);
//                    }
//                })
//                .show();
        sPref = context.getSharedPreferences("MyPref", MODE_PRIVATE);
        Boolean savedBoolean = sPref.getBoolean("Key", false);
        if (savedBoolean){
            openPDFThroughGoogleDrive(context, pdfUrl);
        } else {
            new MaterialDialog.Builder(context)
                    .iconRes(R.drawable.ic_cloud_queue_black_24dp)
                    .limitIconToDefaultSize()
                    .title(R.string.pdf_show_online_dialog_title)
                    .content(R.string.pdf_show_online_dialog_question)
                    .positiveText(R.string.pdf_show_online_dialog_button_yes)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            openPDFThroughGoogleDrive(context, pdfUrl);
                        }
                    })
                    .negativeText(R.string.pdf_show_online_dialog_button_no)
                    .onAny(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            saveCheckbox(context, dialog.isPromptCheckBoxChecked());
                        }
                    })
                    .checkBoxPromptRes(R.string.dont_ask_again, false, null)
                    .show();
        }
    }

    private static void saveCheckbox(Context context, Boolean check) {
        sPref = context.getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putBoolean("Key", check);
        ed.commit();
    }

    /**
     * Launches a browser to view the PDF through Google Drive
     * @param context
     * @param pdfUrl
     */
    public static void openPDFThroughGoogleDrive(final Context context, final String pdfUrl) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        i.setDataAndType(Uri.parse(GOOGLE_DRIVE_PDF_READER_PREFIX + pdfUrl ), HTML_MIME_TYPE );
        context.startActivity(i);
    }
    /**
     * Open a local PDF file with an installed reader
     * @param context
     * @param localUri
     */
    public static final void openPDF(Context context, Uri localUri ) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        i.setDataAndType( localUri, PDF_MIME_TYPE );
        context.startActivity( i );
    }
    /**
     * Checks if any apps are installed that supports reading of PDF files.
     * @param context
     * @return
     */
    public static boolean isPDFSupported( Context context ) {
        Intent i = new Intent( Intent.ACTION_VIEW );
        final File tempFile = new File( context.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS ), "test.pdf" );
        i.setDataAndType(Uri.fromFile(tempFile), PDF_MIME_TYPE);
        return context.getPackageManager().queryIntentActivities( i, PackageManager.MATCH_DEFAULT_ONLY ).size() > 0;
    }

    public static boolean isNetworkStatusAvialable (Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
        {
            NetworkInfo netInfos = connectivityManager.getActiveNetworkInfo();
            if(netInfos != null)
                if(netInfos.isConnected())
                    return true;
        }
        return false;
    }



    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static int downloadPDF(final Context context, final String pdfUrl) {
        // Get filename
        final String filename = pdfUrl.substring( pdfUrl.lastIndexOf( "/" ) + 1 );
        // The place where the downloaded PDF file will be put
        final File tempFile = new File( context.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS ), filename );
        if ( tempFile.exists() ) {
            // If we have downloaded the file before, just go ahead and show it.
            openPDF( context, Uri.fromFile(tempFile) );
            return a;
        }

        // Show progress dialog while downloading
//        final ProgressDialog progress = ProgressDialog.show(context, context.getString(R.string.pdf_show_local_progress_title), context.getString(R.string.pdf_show_local_progress_content), true);

        // Create the download request
        DownloadManager.Request r = new DownloadManager.Request( Uri.parse( pdfUrl ) );
        r.setDestinationInExternalFilesDir( context, Environment.DIRECTORY_DOWNLOADS, filename );
        final DownloadManager dm = (DownloadManager) context.getSystemService( Context.DOWNLOAD_SERVICE );
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                if ( !progress.isShowing() ) {
//                    return;
//                }
                context.unregisterReceiver( this );

//                progress.dismiss();
                long downloadId = intent.getLongExtra( DownloadManager.EXTRA_DOWNLOAD_ID, -1 );
                Cursor c = dm.query( new DownloadManager.Query().setFilterById( downloadId ) );

                if ( c.moveToFirst() ) {
                    int status = c.getInt( c.getColumnIndex( DownloadManager.COLUMN_STATUS ) );
                    if ( status == DownloadManager.STATUS_SUCCESSFUL ) {
//                        openPDF( context, Uri.fromFile( tempFile ) );
                    }
                }
                c.close();
            }
        };
        context.registerReceiver( onComplete, new IntentFilter( DownloadManager.ACTION_DOWNLOAD_COMPLETE ) );

        // Enqueue the request
        dm.enqueue(r);
        return b;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static boolean downloadInspection(final Context context, final String pdfUrl) {
        // Get filename
        final String filename = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        // The place where the downloaded PDF file will be put
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        if (tempFile.exists()) {
            // If we have downloaded the file before, just go ahead and show it.
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void deleteFile (final Context context, final String pdfUrl, final CircularProgressButton buttonOpen, final CircularProgressButton buttonDelete) {
        // Get filename
        final String filename = pdfUrl.substring(pdfUrl.lastIndexOf("/") + 1);
        // The place where the downloaded PDF file will be put
        final File tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        if (tempFile.exists()) {
            // If we have downloaded the file before, just go ahead and show it.
            tempFile.delete();
            Toast toast = Toast.makeText(context.getApplicationContext(), context.getString(R.string.fileDeleted), Toast.LENGTH_SHORT);
  //          toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
//            opendPDF(context, pdfUrl);
        }
        buttonOpen.setText(R.string.download);
        buttonOpen.setProgress(0);
        buttonDelete.setVisibility(View.GONE);
        return;
    }

}
