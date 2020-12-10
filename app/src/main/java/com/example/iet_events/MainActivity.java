package com.example.iet_events;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iet_events.fragments.DashboardFragment;
import com.example.iet_events.fragments.HomeFragment;
import com.example.iet_events.fragments.ProfileFragment;
import com.example.iet_events.models.Task;
import com.example.iet_events.ui.AdminActivity;
import com.example.iet_events.ui.LoginActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.drawer_layout) DrawerLayout drawer_layout;
    @BindView(R.id.nav_view) NavigationView nav_view;
    @BindView(R.id.main_toolbar) Toolbar main_toolbar;
    @BindView(R.id.qr_code_button) FloatingActionButton qr_code_button;
    @BindView(R.id.nav_bottom_lyt_link) LinearLayout nav_bottom_lyt_link;
    private TextView nav_name_text, nav_mail_text;

    private FirebaseAuth mAuth;
    public static String NAME, ROLE, USER_ID;
    public static List<Task> taskList;
    private SharedPreferences loginPrefs;
    private String name_check;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setSupportActionBar(main_toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer_layout, main_toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer_layout.addDrawerListener(toggle);
        toggle.syncState();

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, new HomeFragment()).commit();
            nav_view.setCheckedItem(R.id.nav_home);
        }
        nav_view.setNavigationItemSelectedListener(this);
        nav_name_text = nav_view.getHeaderView(0).findViewById(R.id.nav_name_text);
        nav_mail_text = nav_view.getHeaderView(0).findViewById(R.id.nav_mail_text);

        mAuth = FirebaseAuth.getInstance();

        qr_code_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrDialogBox();
            }
        });

        nav_bottom_lyt_link.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ietvit.com"))));
    }

    @Override
    public void onStart() {
        super.onStart();

        loginPrefs = getSharedPreferences("LoginInfo", MODE_PRIVATE);
        name_check = loginPrefs.getString("Name", null);
        USER_ID = loginPrefs.getString("UserId", null);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            SharedPreferences.Editor Ed = loginPrefs.edit();

            if(name_check == null) {
                DatabaseReference mRef = FirebaseDatabase.getInstance().getReference("Users");
                mRef.child(USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            NAME = String.valueOf(snapshot.child("Name").getValue());
                            Ed.putString("Name", NAME);
                            ROLE = String.valueOf(snapshot.child("Role").getValue());
                            Ed.putString("Role", ROLE);
                            Ed.commit();
                            nav_name_text.setText(NAME);
                            nav_mail_text.setText(currentUser.getEmail());
                            fetchTasks(USER_ID);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Database Error : " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }else {
                NAME = loginPrefs.getString("Name",null);
                ROLE = loginPrefs.getString("Role",null);
                nav_name_text.setText(NAME);
                nav_mail_text.setText(loginPrefs.getString("Email",null));
                fetchTasks(USER_ID);
            }
        }else{
            sendToLogin();
        }
    }

    public void fetchTasks(String userId){
        taskList = new ArrayList<>();
        DatabaseReference mRef = FirebaseDatabase.getInstance().getReference("Users");
        mRef.child(userId).child("Tasks").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot taskSnap : snapshot.getChildren()) {
                        Task task = taskSnap.getValue(Task.class).withId(taskSnap.getKey());
                        taskList.add(task);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Database Error : " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, new HomeFragment()).commit();
                break;
            case R.id.nav_dashboard:
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, new DashboardFragment()).commit();
                break;
            case R.id.nav_profile:
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, new ProfileFragment()).commit();
                break;
            case R.id.action_visit_website:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hackoff.tech")));
                break;
            case R.id.action_logout:
                mAuth.signOut();
                sendToLogin();
                break;
        }
        drawer_layout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_admin) {
            if(ROLE.equals("Board"))
                startActivity(new Intent(MainActivity.this, AdminActivity.class));
            else
                Toast.makeText(MainActivity.this, "Work harder to be a Board member :)",Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void sendToLogin() {
        SharedPreferences.Editor Ed = loginPrefs.edit();
        Ed.putString("Name", null);
        Ed.putString("UserId", null);
        Ed.putString("Email", null);
        Ed.putString("Role", null);
        Ed.commit();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void qrDialogBox() {
        Dialog updateDialog = new Dialog(this);
        updateDialog.setContentView(R.layout.qr_code_dialog);
        updateDialog.setCancelable(true);
        updateDialog.show();

        ImageView qr_code = updateDialog.findViewById(R.id.qr_code_image);

//        String qr_text = "Get Ready for the best of IET";
        String qr_text = NAME + "\n" + mAuth.getCurrentUser().getEmail();
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qr_text, BarcodeFormat.QR_CODE,450,450);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qr_code.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}