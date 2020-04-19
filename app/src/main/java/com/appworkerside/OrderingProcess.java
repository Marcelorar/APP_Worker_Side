package com.appworkerside;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appworkerside.utils.ChatMessage;
import com.appworkerside.utils.Pedido;
import com.appworkerside.utils.WorkerLocation;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderingProcess extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseListAdapter<ChatMessage> adapter;
    private ListView listOfMessages;
    private SwipeRefreshLayout swr;
    private Button aceptarWork;
    private Button cancelarWork;
    private WorkerLocation currentWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordering_process);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        Log.i("chat code", getIntent().getStringExtra("orderCode"));
        FloatingActionButton fab =
                findViewById(R.id.fab);
        cancelarWork = findViewById(R.id.cancelarWork);

        currentWorker = (WorkerLocation) getIntent().getSerializableExtra("currentUser");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myRef = database.getReference("mensajes/" + getIntent().getStringExtra("orderCode").trim());
                EditText input = findViewById(R.id.input);

                // Read the input field and push a new instance
                // of ChatMessage to the Firebase database
                myRef.push()
                        .setValue(new ChatMessage(input.getText().toString(),
                                currentWorker.getWorkUser().getNombre() + " " + ((WorkerLocation) getIntent().getSerializableExtra("currentUser")).getWorkUser().getApellido())
                        );

                // Clear the input
                input.setText("");
                displayChatMessages();
            }
        });

        listOfMessages = findViewById(R.id.list_of_messages);

        displayChatMessages();
        swr = findViewById(R.id.swiperefresh);
        swr.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        displayChatMessages();
                        swr.setRefreshing(false);
                    }
                }
        );

        cancelarWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef = database.getReference("workers");
                myRef
                        .child(currentWorker.getWorkUser()
                                .getUsername())
                        .child("visible").setValue(true)
                        .addOnSuccessListener(new
                                                      OnSuccessListener<Void>() {
                                                          @Override
                                                          public void onSuccess(Void aVoid) {
                                                              myRef = database.getReference("workers");
                                                              myRef
                                                                      .child(currentWorker
                                                                              .getWorkUser()
                                                                              .getUsername())
                                                                      .child("contratado").setValue("").addOnSuccessListener(new
                                                                                                                                     OnSuccessListener<Void>() {
                                                                                                                                         @Override
                                                                                                                                         public void onSuccess(Void aVoid) {


                                                                                                                                             FirebaseFirestore.getInstance()
                                                                                                                                                     .collection("contratos")
                                                                                                                                                     .document(getIntent().getStringExtra("orderCode").trim()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                                                                                                 @Override
                                                                                                                                                 public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                                                                                                     myRef = database.getReference("clients");
                                                                                                                                                     myRef
                                                                                                                                                             .child((documentSnapshot.toObject(Pedido.class).getClient()).getCorreo())
                                                                                                                                                             .child("contratando").setValue("");
                                                                                                                                                     Intent intent = new Intent(OrderingProcess.this, MapsActivity.class);
                                                                                                                                                     startActivity(intent);
                                                                                                                                                 }
                                                                                                                                             });


                                                                                                                                         }
                                                                                                                                     });


                                                          }
                                                      });


            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    private void getCurrentClient() {

    }


    private void displayChatMessages() {
        myRef = database.getReference().child("mensajes/" + getIntent().getStringExtra("orderCode").trim());
        Query q = myRef;
        FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                .setQuery(q, ChatMessage.class)
                .setLayout(R.layout.message)
                .build();

        adapter = new FirebaseListAdapter<ChatMessage>(options) {
            @Override
            protected void populateView(View v, ChatMessage model, int position) {
                Log.i("Fill test", "//////////");
                Log.i("Fill test", model.getMessageText());
                // Get references to the views of message.xml
                TextView messageText = v.findViewById(R.id.message_text);
                TextView messageUser = v.findViewById(R.id.message_user);
                TextView messageTime = v.findViewById(R.id.message_time);

                // Set their text
                messageText.setText(model.getMessageText());
                messageUser.setText(model.getMessageUser());

                // Format the date before showing it
                messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
                        model.getMessageTime()));
            }
        };
        adapter.startListening();
        listOfMessages.setAdapter(adapter);
    }
}
