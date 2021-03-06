package com.example.android.meridian.sos;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.meridian.R;
import com.shamanland.fab.FloatingActionButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

//import android.support.v7.app.ActionBarActivity;

public class SOSActivity extends AppCompatActivity {

    private static final int RESULT_PICK_CONTACT = 10;
    public static final String SETTINGS_FILE = "SETTINGS_FILE";
    private static final String MESSAGE_TEXT_KEY =  "messageText";

    private Button btnPanic;
    private FloatingActionButton addContact;
    private ListView listContacts;
    private SlidingUpPanelLayout contactsDrawer;
    private Locator locator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sos_activity);
        locator = com.example.android.meridian.sos.Locator.getLocator(this);
        initGui();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        locator.requestUpdate(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sos_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit_message) {
            editMessageContent();
        }
        if (id == R.id.action_show_position) {
            showPositionInfo();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (contactsDrawer.isPanelExpanded()){
            contactsDrawer.collapsePanel();
        } else super.onBackPressed();
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Source").setMessage("Select the source to import phone numbers")
                .setNegativeButton("Contacts", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseContact();
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Phone Number", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        choosePhoneNumber();
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    private void editMessageContent() {
        new com.example.android.meridian.sos.InsertMessageContentDialog(this).show();
    }

    private void showPositionInfo() {
        try {
            new com.example.android.meridian.sos.PositionInfoDialog(locator.getLocation(), SOSActivity.this).show();
        } catch (com.example.android.meridian.sos.Locator.NoPositionProvidersException e) {
            locator.showSettingsAlert();
        }
    }

    private void choosePhoneNumber() {
        new com.example.android.meridian.sos.PersonAddNumberDialog(SOSActivity.this, listContacts).show();
    }

    private void chooseContact() {
        Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(i, RESULT_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == RESULT_PICK_CONTACT) {

                Uri contractData = data.getData();
                com.example.android.meridian.sos.ContactRetriever cr = new com.example.android.meridian.sos.ContactRetriever(getApplicationContext(), contractData);
                com.example.android.meridian.sos.Person p = cr.getPerson();

                if (p == null) Toast.makeText(this, "Phone number not found!", Toast.LENGTH_SHORT).show();

                else {
                    com.example.android.meridian.sos.PersonManager.savePerson(p, getApplicationContext());
                    listContacts.setAdapter(new com.example.android.meridian.sos.PersonAdapter(this, com.example.android.meridian.sos.PersonManager.getSavedPersons(this)));
                }
            }
        }
    }


    private void initGui() {

        btnPanic = (Button) findViewById(R.id.btn_panic);
        btnPanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Location loc = locator.getLocation();
                    SharedPreferences sp = getSharedPreferences(SETTINGS_FILE, 0);

                    String messageText = sp.getString(MESSAGE_TEXT_KEY, null);

                    if (messageText == null) {
                        messageText = "I need help come ASAP!";
                    }

                    messageText += "\nCOORDINATES: \nLat: " + loc.getLatitude() + " \nLng: " + loc.getLongitude() + "\n\nADDRESS: \n" + com.example.android.meridian.sos.Locator.getAddressFromLocation(loc, getApplicationContext());
                    //Toast.makeText(getApplicationContext(), messageText, Toast.LENGTH_LONG).show();

                    SmsManager smsMan = SmsManager.getDefault();
                    ArrayList<com.example.android.meridian.sos.Person> recipients = com.example.android.meridian.sos.PersonManager.getSavedPersons(getApplicationContext());
                    for (com.example.android.meridian.sos.Person p : recipients){
                        smsMan.sendTextMessage(p.getNumber(), null, messageText, null, null);
                    }

                    Toast.makeText(getApplicationContext(), "Messages Sent", Toast.LENGTH_LONG).show();
                } catch (com.example.android.meridian.sos.Locator.NoPositionProvidersException e) {
                    locator.showSettingsAlert();
                }
            }
        });

        addContact = (FloatingActionButton) findViewById(R.id.fab_action_add_contact);
        addContact.setImageResource(R.drawable.ic_action_content_add);
        addContact.setSize(FloatingActionButton.SIZE_NORMAL);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddContactDialog();
            }
        });

        listContacts = (ListView) findViewById(R.id.contacts_list);
        contactsDrawer = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        contactsDrawer.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                addContact.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPanelCollapsed(View view) {
                addContact.setVisibility(View.VISIBLE);
            }


            @Override
            public void onPanelExpanded(View view) {

            }

            @Override
            public void onPanelAnchored(View view) {

            }

            @Override
            public void onPanelHidden(View view) {

            }
        });

        listContacts.setAdapter(new com.example.android.meridian.sos.PersonAdapter(getApplicationContext(), com.example.android.meridian.sos.PersonManager.getSavedPersons(getApplicationContext())));
    }
}
