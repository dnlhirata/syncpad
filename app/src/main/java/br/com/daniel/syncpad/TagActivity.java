package br.com.daniel.syncpad;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import br.com.daniel.syncpad.firebase.FirebaseHelper;
import br.com.daniel.syncpad.model.Tag;

public class TagActivity extends AppCompatActivity {
    private DatabaseReference firebaseReference;
    private EditText tagName;
    private EditText tagContent;
    private String tagId;
    private ArrayList<Tag> tags;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);

        FirebaseHelper fbHelper = new FirebaseHelper();
        firebaseReference = fbHelper.configuraFirebase();

        tagName = findViewById(R.id.tag_name);
        tagName.setOnFocusChangeListener(tagNameListener);
        tagContent = findViewById(R.id.tag_content);
        tagContent.addTextChangedListener(tagContentWatcher);

        Intent intent = getIntent();
        tags = (ArrayList) intent.getSerializableExtra("tags");

    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseReference.addValueEventListener(tagContentFirebaseListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tag, menu);
        return true;
    }

    //Itens do menu superior
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_tag:
                String utcDateTime = getUTCDateTime();
                salvaTag(tagId, utcDateTime);
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public TextWatcher tagContentWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (timer != null) {
                timer.cancel();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String utcDateTime = getUTCDateTime();
                    salvaTag(tagId, utcDateTime);
                }
            }, 500);
        }
    };

    private String getUTCDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z");
        ZonedDateTime dateTime = ZonedDateTime.now();
        return dateTime.withZoneSameInstant(ZoneOffset.UTC).format(formatter);
    }

    //Recupera o conteúdo da tag após mudar o foco da EditView (tag_name)
    public View.OnFocusChangeListener tagNameListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            tagName = (EditText) v;
            tagId = "";
            if (!hasFocus) {
                for (Tag tag : tags) {
                    if (tag.getName().equals(tagName.getEditableText().toString())) {
                        tagId = tag.getId();
                        tagContent.setText(tag.getContent());
                    }
                }
                if (tagId.isEmpty()) {
                    tagId = firebaseReference.push().getKey();
                }
            }
        }
    };

    public ValueEventListener tagContentFirebaseListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (!tagId.isEmpty()) {
                final String content = dataSnapshot.child(tagId).getValue(Tag.class).getContent();
                tagContent.setText(content);
                tagContent.setSelection(content.length());
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    //Salva a tag no Firebase
    private void salvaTag(String key, String dateTime){
        String name = tagName.getEditableText().toString();
        if (!name.isEmpty()){
            String content = tagContent.getEditableText().toString();
            Tag tag = new Tag(key, name, content, dateTime);
            System.out.println(key);
            firebaseReference.child(key).setValue(tag);
        }
    }

}
