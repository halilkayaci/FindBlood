package com.info.findblood;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    DonorsAdapter donorsAdapter;
    List<Donors> donorsList = new ArrayList<>();
    List<Donors> searchList = new ArrayList<>();
    FloatingActionButton actionButton;
    Toolbar toolbar;
    EditText nameSurname, phone;
    Spinner bloodGroup;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Viewlar baglaniyor.
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.rvFindBlood);
        actionButton = findViewById(R.id.fabAddBlood);

        // internet baglantisi kontrol ediliyor
        if(networkConnection()) toast("İnternet Bağlantınızı Kontrol Edin !");

        // Progress Dialog baglaniyor
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("İnternet Bağlantısı Kontrol Ediliyor. Lütfen Bekleyiniz...");
        dialog.setCancelable(false);
        dialog.show();

        // veriler veritabanindan cekilerek cardviewler olusuyor
        getDonors();

        // ekle butonuna basınca donor ekleyen metod.
        actionButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("InflateParams")
            @Override
            public void onClick(View view) {
                view = getLayoutInflater().inflate(R.layout.popup_menu, null);
                nameSurname = view.findViewById(R.id.etNameSurname);
                phone = view.findViewById(R.id.etPhone);
                bloodGroup = view.findViewById(R.id.spinner);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Donör Ekle");
                builder.setView(view);
                builder.setPositiveButton("EKLE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        boolean state = checkText(nameSurname, phone, bloodGroup);
                        if(state){
                            if(networkConnection()) toast("İnternet Bağlantınızı Kontrol Edin !");
                            else {
                                donorsList.clear();
                                addDonor();
                            }
                            showCards();
                        }
                    }
                });

                builder.setNegativeButton("IPTAL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                builder.create().show();
            }
        });
    }

    // Toolbar üzerindeli searchView nesnesinin eventleri.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_search_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.actionSearch);
        SearchView view = (SearchView) menuItem.getActionView();
        view.setQueryHint("Kan Grubu Ara");
        view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchDatabase(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchList.clear();
                searchDatabase(s);
                return true;
            }
        });

        menuItem = menu.findItem(R.id.actionLogOut);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                // Logout olan eventleri
                auth.signOut();
                Intent back = new Intent(MainActivity.this, Login.class);
                startActivity(back);
                finish();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    // Edittextleri kontrol eden metod.
    public boolean checkText(EditText nameSurname, EditText phone, Spinner bloodGroup){

        if(TextUtils.isEmpty(nameSurname.getText().toString().trim()) || TextUtils.isEmpty(phone.getText().toString().trim())) {
            toast("Bilgileri Eksiksiz Olarak Giriniz !");
            return false;
        }else if(bloodGroup.getSelectedItemPosition() == 0){
            toast("Kan Grubu Seçiniz !");
            return false;
        } else return true;
    }

    // Toast mesaj yazan metod.
    private void toast(String message){
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean networkConnection() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        return conMgr.getActiveNetworkInfo() == null
                || !conMgr.getActiveNetworkInfo().isAvailable()
                || !conMgr.getActiveNetworkInfo().isConnected();
    }

    // Veri tabaninda search yapan metod
    private void searchDatabase(String s) {
        if(s.equals("")){
            showCards();
            searchList.clear();
        }
        else{
            s = s.trim().toUpperCase();
            searchList.clear();
            firebaseUser = auth.getCurrentUser();
            databaseReference = firebaseDatabase.getReference().child("users").child(Objects.requireNonNull(auth.getUid())).child("donors");
            Query query = databaseReference.orderByChild("bloodGroup").startAt(s).endAt(s + "\uf8ff");//.endAt(s+ "\\u0020");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                        if(snapshot.exists()) {
                            Donors donor = snapshot.getValue(Donors.class);
                            searchList.add(donor);
                        }

                    }
                    // Sonuca gore recyclerview dolduruluyor
                    showSearchCards();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    // Cardlari doldularan metod
    private void showCards(){
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setHasFixedSize(true);
        donorsAdapter = new DonorsAdapter(MainActivity.this, donorsList);
        recyclerView.setAdapter(donorsAdapter);
        donorsAdapter.notifyDataSetChanged();
    }
    // arama sonucuna gore Cardlari doldularan metod
    private void showSearchCards(){
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setHasFixedSize(true);
        donorsAdapter = new DonorsAdapter(MainActivity.this, searchList);
        recyclerView.setAdapter(donorsAdapter);
        donorsAdapter.notifyDataSetChanged();
    }

    // veritabanina ekleme yapan metod
    private void addDonor(){
        // veritabanina kullanici idsine gore donor ekleme yapiliyor
        String donorId = String.valueOf(databaseReference.push().getKey());
        Donors donor = new Donors(donorId,nameSurname.getText().toString().trim(),
                phone.getText().toString().trim(),
                bloodGroup.getSelectedItem().toString().trim());

        firebaseUser = auth.getCurrentUser();
        donorsList.add(donor);
        databaseReference = firebaseDatabase.getReference();
        databaseReference.child("users").child(
                firebaseUser.getUid()).child("donors").child(donorId).setValue(donor);
    }

    // veritabanindan donorler ceken metod
    private void getDonors(){
        firebaseUser = auth.getCurrentUser();
        databaseReference = databaseReference.child("users").child(firebaseUser.getUid()).child("donors");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                donorsList.clear();
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        Donors donor = snapshot.getValue(Donors.class);
                        donorsList.add(donor);
                    }
                }
                dialog.dismiss();
                // Veri tabanindan kullanici adina gore veriler cekilip ecycler view dolduruluyor.
                showCards();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
