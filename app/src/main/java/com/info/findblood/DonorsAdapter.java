package com.info.findblood;

import android.Manifest;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import java.util.List;

public class DonorsAdapter extends RecyclerView.Adapter<DonorsAdapter.DonorsViewHolder> {

    private Context context;
    private List<Donors> donorsList;
    private int checkPermission;
    private String[] groups = {
            "A RH+", "A RH-", "B RH+", "B RH-",
            "AB RH+", "AB RH-", "0 RH+", "0 RH-"
    };
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser firebaseUser = auth.getCurrentUser();
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();


    public DonorsAdapter(Context context, List<Donors> donorsList) {
        this.context = context;
        this.donorsList = donorsList;
    }

    @NonNull
    @Override
    public DonorsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        // Cardview tasarimi baglaniyor.
        View layout = LayoutInflater.from(context).inflate(R.layout.rv_blood, viewGroup, false);
        return new DonorsViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull final DonorsViewHolder donorsViewHolder, int i) {
       // Carview textleri dolduruluyor
        donorsViewHolder.name.setText(donorsList.get(i).getNameSurname());
        donorsViewHolder.phone.setText(donorsList.get(i).getPhone());
        donorsViewHolder.bloodGroup.setText(donorsList.get(i).getBloodGroup());
        donorsViewHolder.cardDesign.setAnimation(
                AnimationUtils.loadAnimation(
                        context,
                        R.anim.cardanim
                ));

        final String uuid = donorsList.get(i).getId().trim();
        final String bgid = donorsList.get(i).getBloodGroup().trim();

        // CardView longPress metodu.
        donorsViewHolder.cardDesign.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                // Popup menu aciliyor.
                PopupMenu menu = new PopupMenu(context, view);
                menu.getMenuInflater().inflate(R.menu.card_menu, menu.getMenu());
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        // Menu uzerinden secilen item yakalaniyor.
                        switch (menuItem.getItemId()){

                            case R.id.actionCall:
                                // Arama yapılıyor
                                checkPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE);
                                if(checkPermission != PackageManager.PERMISSION_GRANTED){
                                    ActivityCompat.requestPermissions((Activity)context,
                                            new String[]{Manifest.permission.CALL_PHONE},
                                            100);
                                    return true;
                                }else{
                                    Intent call = new Intent(Intent.ACTION_CALL);
                                    call.setData(Uri.parse("tel:" + donorsViewHolder.phone.getText()));
                                    context.startActivity(call);
                                    return true;
                                }

                            case R.id.actionEdit:
                                // Donor bilgiler duzenlenip veri tabaninda update islemi yapiliyor.
                                View view = View.inflate(context, R.layout.popup_menu, null);
                                final EditText nameSurname = view.findViewById(R.id.etNameSurname);
                                final EditText phone = view.findViewById(R.id.etPhone);
                                final Spinner bloodGroup = view.findViewById(R.id.spinner);
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                nameSurname.setText(donorsViewHolder.name.getText());
                                phone.setText(donorsViewHolder.phone.getText());
                                int spIndex = 0;
                                for (int i = 0; i < groups.length; i++) {
                                    if (bgid.equals(groups[i])) {
                                        spIndex = i + 1;
                                        break;
                                    }
                                }
                                bloodGroup.setSelection(spIndex);

                                builder.setMessage("Donör Düzenle");
                                builder.setView(view);

                                builder.setPositiveButton("DUZENLE", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if(((MainActivity)context).checkText(nameSurname, phone, bloodGroup)) {
                                            //Veri tabani guncellemesi yapiliyor
                                            databaseReference = databaseReference.child("users").child(firebaseUser.getUid()).child("donors");
                                            Query query = databaseReference.orderByChild("id").equalTo(uuid);
                                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                        snapshot.getRef().child("nameSurname").setValue(nameSurname.getText().toString());
                                                        snapshot.getRef().child("phone").setValue(phone.getText().toString());
                                                        snapshot.getRef().child("bloodGroup").setValue(bloodGroup.getSelectedItem().toString());
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    }
                                });

                                builder.setNegativeButton("IPTAL", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });

                                builder.create().show();
                                return true;

                            default:
                                return false;
                        }
                    }
                });

                menu.show();
                return true;
            }
        });


        // CardView üzerine tıklanınca arama yapılıyor
        donorsViewHolder.cardDesign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Arama yapılıyor
                checkPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE);
                if(checkPermission != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions((Activity)context,
                            new String[]{Manifest.permission.CALL_PHONE},
                            100);
                }else{
                    Intent call = new Intent(Intent.ACTION_CALL);
                    call.setData(Uri.parse("tel:" + donorsViewHolder.phone.getText()));
                    context.startActivity(call);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return donorsList.size();
    }

    public class DonorsViewHolder extends RecyclerView.ViewHolder {

        TextView name, phone, bloodGroup;
        CardView cardDesign;

        public DonorsViewHolder(@NonNull View itemView) {
            super(itemView);
            // Cardview uzerindeki viewler baglaniyor.
            name = itemView.findViewById(R.id.tv_Name);
            phone = itemView.findViewById(R.id.tv_Phone);
            bloodGroup = itemView.findViewById(R.id.tv_BloogGroup);
            cardDesign = itemView.findViewById(R.id.cardDesign);
        }
    }
}