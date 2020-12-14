package com.cfdemo.receiptreader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.cfdemo.receiptreader.StorageHelper.*;

import org.w3c.dom.Text;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileSetup();
    }

    private void profileSetup () {
        TextView txtName = findViewById(R.id.txt_name);
        TextView txtCredit = findViewById(R.id.txt_credit);

        txtName.setText("User: " + StorageHelper.getName(this));
        txtCredit.setText("Credit: " + StorageHelper.getCredit(this)+"");
    }
}