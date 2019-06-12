package com.info.findblood;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;


public class Login extends AppCompatActivity {
    EditText etMail, etPass;
    Button btnSignIn;
    TextView tvRegister, tvOther, tvForgotPass;
    ImageView imgLogo;
    TextInputLayout inputMail, inputPass;
    private FirebaseAuth auth;
    private static final int RC_SIGN_IN = 9001;
    SignInButton signInButton;
    private GoogleApiClient googleApiClient;
    Animation upToDown, downToUp;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // widget nesneleri baglaniyor
        inputMail = findViewById(R.id.inputMailReg);
        inputPass = findViewById(R.id.inputPassReg);
        etMail = findViewById(R.id.etMail);
        etPass = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvRegister = findViewById(R.id.tvRegister);
        signInButton = findViewById(R.id.btnGooleSign);
        imgLogo = findViewById(R.id.imgLogoReg);
        tvOther = findViewById(R.id.tvOther);
        tvForgotPass = findViewById(R.id.tvForgotPass);

        upToDown = AnimationUtils.loadAnimation(Login.this, R.anim.uptodown);
        downToUp = AnimationUtils.loadAnimation(Login.this, R.anim.downtoup);

        // Animasyonlar widgetlere baglaniyor
        imgLogo.setAnimation(upToDown);
        inputMail.setAnimation(upToDown);
        inputPass.setAnimation(upToDown);
        tvForgotPass.setAnimation(upToDown);

        tvOther.setAnimation(downToUp);
        btnSignIn.setAnimation(downToUp);
        signInButton.setAnimation(downToUp);
        tvRegister.setAnimation(downToUp);

        // Progress Dialog baglaniyor
        dialog = new ProgressDialog(Login.this);
        dialog.setMessage("Lütfen Bekleyiniz...");
        dialog.setCancelable(false);

        //FirebaseAuth sinifinin referans oldugu nesneleri kullanabilmek icin getInstance metodu kullanliyor
        auth = FirebaseAuth.getInstance();

        // Sifremi unuttum yapilandirmasi ve eventleri
        tvForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!etMail.getText().toString().trim().isEmpty()){
                    dialog.show();
                    auth.sendPasswordResetEmail(etMail.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(!networkConnection()){
                                dialog.dismiss();
                                toast("İnternet bağlantınızı kontrol ediniz !");
                            }
                            else if(task.isSuccessful()){
                                dialog.dismiss();
                                toast("Şifre sıfırlama bağlantısı eposta adresinize gönderilmiştir");
                            }
                            else{
                                dialog.dismiss();
                                toast("Eposta adresine uygun kullanıcı bulunamadı !");
                            }
                        }
                    });
                }
                else{
                    toast("Lütfen bir eposta adresi giriniz !");
                }
            }
        });

        // kayit ekrani aciliyor
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent(Register.class);
            }
        });

        // Google Sign in Options yapilandirmasi
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(Login.this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        toast("Baglantı basarısız !");
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(networkConnection())
                    signIn();
                else toast("İnternet Bağlantınızı Kontrol Edin !");
            }
        });

        //Gecerli bir yetkilendirme olup olmadigi kontrol ediliyor. remember me olarak kullanılıyor
        if(auth.getCurrentUser() != null){
            intent(MainActivity.class);
            finish();
        }

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(controlText(etMail, etPass)){
                    if(!networkConnection()) toast("İnternet Bağlantınızı Kontrol Edin !");
                    else {
                        dialog.show();
                        auth.signInWithEmailAndPassword(etMail.getText().toString().trim(), etPass.getText().toString().trim())
                                .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {

                                        if (task.isSuccessful()) {
                                            dialog.dismiss();
                                            intent(MainActivity.class);
                                            finish();
                                        } else {
                                            dialog.dismiss();
                                            toast("Giriş Başarısız ! Bilgilerinizi Kontrol Ediniz");
                                        }
                                    }
                                });
                    }
                }
            }
        });

    }

    // konrolleri yapan metod
    private boolean controlText(EditText etRegMail, EditText etRegPass){
        String mail = etRegMail.getText().toString().trim();
        String pass = etRegPass.getText().toString().trim();
        if(TextUtils.isEmpty(mail) || TextUtils.isEmpty(pass)){
            toast("Bilgileri eksiksiz doldurunuz !");
            return false;
        }
        else return true;
    }

    // Toast mesaj yazan metod.
    private void toast(String message){
        Toast.makeText(Login.this, message, Toast.LENGTH_SHORT).show();
    }

    // intent metodu
    private void intent(Class<?> cls){
        Intent intent = new Intent(getApplicationContext(), cls);
        startActivity(intent);
    }

    //Google ile Oturum acma islemleri
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In basarili oldugunda Firebase ile yetkilendir
                GoogleSignInAccount account = result.getSignInAccount();
                assert account != null;
                firebaseAuthWithGoogle(account);

            } else {
                // Google Sign In hatası.
                toast("Google ile Oturum Açma Hatası !");
            }
        }
    }

    // Firebase ile yetkilendirme işlemi gerceklestiriliyor
    private void firebaseAuthWithGoogle(GoogleSignInAccount account){
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {@Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {
                            toast("Yetkilendirme hatası.");
                            }else {
                                startActivity(new Intent(Login.this, MainActivity.class));
                                finish();
                            }
                        }
                    });

    }

    private boolean networkConnection() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        return conMgr.getActiveNetworkInfo() != null
                && conMgr.getActiveNetworkInfo().isAvailable()
                && conMgr.getActiveNetworkInfo().isConnected();
    }

}
