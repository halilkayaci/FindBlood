package com.info.findblood;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends AppCompatActivity {

    EditText etRegMail, etRegPass;
    TextInputLayout inputMailReg, inputPassReg;
    Button btnSignup;
    private FirebaseAuth auth;
    ProgressDialog dialog;
    ImageView imgLogoReg;
    Animation rightToLeft, leftToRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // widgetler baglaniyor
        etRegMail = findViewById(R.id.etRegMail);
        etRegPass = findViewById(R.id.etRegPassword);
        btnSignup = findViewById(R.id.btnSignUp);
        imgLogoReg = findViewById(R.id.imgLogoReg);
        inputMailReg = findViewById(R.id.inputMailReg);
        inputPassReg = findViewById(R.id.inputPassReg);

        rightToLeft = AnimationUtils.loadAnimation(Register.this, R.anim.righttoleft);
        leftToRight = AnimationUtils.loadAnimation(Register.this, R.anim.lefttoright);

        // Animasyon widgetlere baglaniyor

        inputMailReg.setAnimation(leftToRight);
        inputPassReg.setAnimation(leftToRight);

        imgLogoReg.setAnimation(rightToLeft);
        btnSignup.setAnimation(rightToLeft);


        // Progress Dialog baglaniyor
        dialog = new ProgressDialog(Register.this);
        dialog.setMessage("Lütfen Bekleyiniz...");
        dialog.setCancelable(false);

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if(controlText(etRegMail, etRegPass)){
                    dialog.show();
                    //FirebaseAuth ile email,parola parametrelerini kullanarak yeni bir kullanıcı oluşturuluyor
                    if(networkConnection()){
                        dialog.dismiss();
                        toast("İnternet Bağlantınızı Kontrol Edin !");
                    }
                    else{
                    auth = FirebaseAuth.getInstance();
                    auth.createUserWithEmailAndPassword(etRegMail.getText().toString().trim(), etRegPass.getText().toString().trim())
                            .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(!task.isSuccessful()){
                                        if(isValidEmail(etRegMail.getText().toString().trim())) {
                                            dialog.dismiss();
                                            toast("Bu Epostaya Ait Bir Kullanıcı Zaten Mevcut !");
                                        }
                                        else {
                                            dialog.dismiss();
                                            toast("Geçersiz Eposta Formatı");
                                        }
                                    }
                                    else {
                                        dialog.dismiss();

                                        //Ekrana bilgi yazısı bastırılıyor
                                        Snackbar.make(view, "Kullanıcı Kaydı Başarılı", 60000)
                                                .setAction("GİRİŞ YAP", new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        Intent back = new Intent(Register.this, Login.class);
                                                        startActivity(back);
                                                        finish();
                                                    }
                                                }).show();
                                    }
                                }
                            });
                    }
                }
            }
        });
    }

    // Toast mesaj yazan metod.
    private void toast(String message){
        Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();
    }

    // konrolleri yapan metod
    private boolean controlText(EditText etRegMail, EditText etRegPass){
        String mail = etRegMail.getText().toString().trim();
        String pass = etRegPass.getText().toString().trim();
        if(TextUtils.isEmpty(mail) || TextUtils.isEmpty(pass)){
            toast("Bilgileri eksiksiz doldurunuz !");
            return false;
        }
        else if(pass.length() < 6){
            toast("Sifre en az 6 haneli olmalıdır !");
            return false;
        }
        else return true;
    }

    // internet baglantisi kontrol ediliyor
    private boolean networkConnection() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        return conMgr.getActiveNetworkInfo() == null
                || !conMgr.getActiveNetworkInfo().isAvailable()
                || !conMgr.getActiveNetworkInfo().isConnected();
    }

    // Email formatı kontrol eden metod
    private static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
