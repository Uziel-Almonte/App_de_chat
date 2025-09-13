package com.example.app_de_chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText inputUser;
    private EditText inputPass;
    private Button btnLogin;
    private Button btnGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthManager.isLoggedIn(this)) {
            goToMainAndClear();
            return;
        }

        setContentView(R.layout.activity_login);

        inputUser = findViewById(R.id.inputUser);
        inputPass = findViewById(R.id.inputPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        btnLogin.setOnClickListener(v -> {
            String user = inputUser.getText() != null ? inputUser.getText().toString().trim() : "";
            String pass = inputPass.getText() != null ? inputPass.getText().toString() : "";

            if (TextUtils.isEmpty(user) || TextUtils.isEmpty(pass)) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!AuthManager.isUserRegistered(this)) {
                Toast.makeText(this, R.string.error_no_user_registered, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = AuthManager.login(this, user, pass);
            if (ok) {
                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                goToMainAndClear();
            } else {
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
            }
        });

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void goToMainAndClear() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
