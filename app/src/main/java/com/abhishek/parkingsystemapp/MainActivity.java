package com.abhishek.parkingsystemapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.abhishek.parkingsystemapp.Dialogs.BookingDialog;
import com.abhishek.parkingsystemapp.Dialogs.CheckoutDialog;
import com.abhishek.parkingsystemapp.Dialogs.WalletDialog;
import com.abhishek.parkingsystemapp.Fragments.BookingFragment;
import com.abhishek.parkingsystemapp.Fragments.HistoryFragment;
import com.abhishek.parkingsystemapp.Fragments.ProfileFragment;
import com.abhishek.parkingsystemapp.Fragments.WalletFragment;
import com.abhishek.parkingsystemapp.Models.AppUser;
import com.abhishek.parkingsystemapp.Models.ParkingSlot;
import com.abhishek.parkingsystemapp.Models.UserHistory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity
        implements WalletDialog.WalletDialogListener,
        BookingDialog.BookingDialogListener,
        CheckoutDialog.CheckoutDialogListener{

    private BottomNavigationView bnNav;
    private Fragment selectorFragment;

    private String fragmentTag = "BOOKING-FRAGMENT";

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    AppUser userRealTimeInstance;
    //boolean previousParked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.toobar_signout) { //SignOut with Firebase
                    firebaseAuth = FirebaseAuth.getInstance();
                    firebaseAuth.signOut();
                    startActivity(new Intent(MainActivity.this,
                            com.abhishek.parkingsystemapp.StartActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                }
                return true;
            }
        });

        bnNav = findViewById(R.id.bnNav);
        bnNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {

                switch (item.getItemId()){

                    case R.id.nav_profile: selectorFragment = new ProfileFragment();
                                            fragmentTag = "PROFILE-FRAGMENT";
                                            break;

                    case R.id.nav_booking: selectorFragment = new BookingFragment();
                                            fragmentTag = "BOOKING-FRAGMENT";
                                            triggerSnapshot();
                                            break;

                    case R.id.nav_wallet: selectorFragment = new WalletFragment();
                                            fragmentTag = "WALLET-FRAGMENT";
                                            break;

                    case R.id.nav_history: selectorFragment = new HistoryFragment();
                                            fragmentTag = "HISTORY-FRAGMENT";
                                            break;

                }

                if (selectorFragment != null)
                {
                    Log.d("FRAGMENT IN :; ", fragmentTag);
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.fragment_container, selectorFragment, fragmentTag)
                            .commit();
                }

                return true;
            }
        });

        Bundle intent = getIntent().getExtras();
        if(intent == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BookingFragment()).commit();
        }

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable @org.jetbrains.annotations.Nullable DocumentSnapshot value,
                                        @Nullable @org.jetbrains.annotations.Nullable FirebaseFirestoreException error) {
                        if(value != null && value.exists()){
                            Log.d("User update : ", "Current data: " + value.getData());
                            userRealTimeInstance = value.toObject(AppUser.class);
                            Log.d("User update : ", "Current data: " + userRealTimeInstance.getName() + userRealTimeInstance.isIsReady());
                            if(!userRealTimeInstance.isIsReady() && userRealTimeInstance.isParked() && !userRealTimeInstance.getTransactionId().isEmpty()){
                                //Change Arrival time in User History
                                Log.d("Changing arrival time ", "" + Timestamp.now());
                                firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                                        .collection("HISTORY").document(userRealTimeInstance.getTransactionId())
                                        .update("arrival", Timestamp.now())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull @NotNull Task<Void> task) {
                                                Toast.makeText(MainActivity.this, "Hello!! You've arrived!!", Toast.LENGTH_SHORT)
                                                        .show();
                                            }
                                        });
                            }
                            else if(userRealTimeInstance.isIsReady() && !userRealTimeInstance.getTransactionId().isEmpty()){
                                //Compute Amount
                                Log.d("Computing check out!! ", "Checking out!!");
                                Toast.makeText(MainActivity.this, "Checking out!!...", Toast.LENGTH_SHORT).show();
                                checkoutSlot(userRealTimeInstance.isIsCancel());
                            }

                            if(userRealTimeInstance.isParked() || (userRealTimeInstance.getSlotNumber().isEmpty() && userRealTimeInstance.getTransactionId().isEmpty())){
                                //Show cancel button
                                BookingFragment myFragment = (BookingFragment)getSupportFragmentManager().findFragmentByTag("BOOKING-FRAGMENT");
                                Log.d("FRAGMENT IN  :; ", fragmentTag + " (REALTIME) " + myFragment);
                                if(myFragment != null && myFragment.isVisible())
                                    MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.fragment_container)
                                            .getView().findViewById(R.id.btnCancelSlot).setVisibility(View.GONE);
                                //I'm so sorry if your app crashes!
                            }
                            else{
                                BookingFragment myFragment = (BookingFragment)getSupportFragmentManager().findFragmentByTag("BOOKING-FRAGMENT");
                                Log.d("FRAGMENT IN  :; ", fragmentTag + " (REALTIME) " + myFragment);
                                if(myFragment != null && myFragment.isVisible())
                                    MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.fragment_container)
                                            .getView().findViewById(R.id.btnCancelSlot).setVisibility(View.VISIBLE);
                            }

//                            if(previousParked != userRealTimeInstance.isParked())
//                                previousParked = userRealTimeInstance.isParked();
                        }
                        else {
                            Log.d("User update : ", "Current data: null");
                        }
                    }
                });
    }

    private void triggerSnapshot() {
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .update("irrelevant", FieldValue.increment(1));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_signout, menu);
        return true;
    }

    /////
    private void checkoutSlot(boolean cancel) {
        //Confirm if user is exiting
        //Calculate Fair
        //If wallet has sufficient balance ://Update history
                                            //Update Parking Slot document
                                            //Set user slot number to empty and transationId to empty and deduct amount from wallet
        //Else : Do nothing!!

        if(!userRealTimeInstance.getSlotNumber().isEmpty() && !userRealTimeInstance.getTransactionId().isEmpty() && !cancel)
            openCheckoutDialog(cancel);
        else{
            Toast.makeText(MainActivity.this, "Your booking has been cancelled!!", Toast.LENGTH_SHORT).show();
            openCheckoutDialog(cancel);
        }

        updateUserToNotReady();

    }

    private void updateUserToNotReady() {

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .update("isReady", false,
                        "parked", false,
                        "isCancel", false)
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task task) {
                        Log.d("user to not ready!!", " Done!! Also not cancel!!");
                    }
                });
    }

    private void openCheckoutDialog(boolean cancel) {
        //progressBar.setVisibility(View.VISIBLE);
        final UserHistory[] history = {new UserHistory()};
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .collection("HISTORY").document(userRealTimeInstance.getTransactionId())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null){
                            history[0] = task.getResult().toObject(UserHistory.class);
                            if(!cancel){
                                CheckoutDialog checkout = new CheckoutDialog(userRealTimeInstance, history[0]);//, progressBar);
                                checkout.show(getSupportFragmentManager(), "Checkout Dialog");
                            }
                            else{
                                payment(history[0], userRealTimeInstance);
                            }
                        }
                    }
                });
    }
    //////


    @Override
    public void recharge(double amount, AppUser user) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .update("wallet", user.getWallet())
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task task) {
                        if(task.isSuccessful()){
                            //tvAmount.setText(user.getWallet());
                            Toast.makeText(MainActivity.this, "Recharge successful for ₹" + amount, Toast.LENGTH_SHORT).show();
                        }
                        //progressBar.setVisibility(View.INVISIBLE);
                        selectorFragment = new WalletFragment();
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectorFragment)
                                .commit();
                    }
                });
    }

    @Override
    public void bookParking(ParkingSlot slot, AppUser user) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        user.setSlotNumber(slot.getSlotId());
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .update("slotNumber", user.getSlotNumber())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        UserHistory history = new UserHistory("", Timestamp.now(), null, null, 50);
                        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                                .collection("HISTORY")
                                .add(history)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        history.setTransactionId(documentReference.getId());
                                        updateHistory(history);
                                        updateSlot(history, slot, user);

                                        selectorFragment = new BookingFragment();
                                        fragmentTag = "BOOKING-FRAGMENT";
                                        getSupportFragmentManager().beginTransaction()
                                                .replace(R.id.fragment_container, selectorFragment, fragmentTag)
                                                .commit();

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull @NotNull Exception e) {

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {

                    }
                });
    }


    private void updateHistory(UserHistory history) {
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .collection("HISTORY").document(history.getTransactionId())
                .update("transactionId", history.getTransactionId())
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task task) {
                        if(task.isSuccessful()){

                            //Updating Transaction Id in user document
                            firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                                    .update("transactionId", history.getTransactionId())
                                    .addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull @NotNull Task task) {
                                            if(task.isSuccessful()){
                                                Log.d("User update :: ", "True : " + history.getTransactionId());
                                            }
                                        }
                                    });

                            Log.d("History update :: ", "True : " + history.getTransactionId());
                        }
                        else {
                            Log.e("History update :: ", "False : " + history.getTransactionId());
                        }
                    }
                });
    }

    private void updateSlot(UserHistory history, ParkingSlot slot, AppUser user) {
        slot.setUserId(firebaseAuth.getCurrentUser().getUid());
        slot.setBooked(true);
        slot.setTransactionId(history.getTransactionId());
        slot.setLicensePlate(user.getLicense());
        slot.setElapsed_timer(true);
        firestore.collection("PARKING").document(slot.getSlotId())
                .set(slot)
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task task) {
                        if(task.isSuccessful()){
                            Toast.makeText(MainActivity.this, "Successfully booked Parking!!", Toast.LENGTH_SHORT).show();
                            Log.d("Parking update :: ", "True : " + slot.getSlotId());
                        }
                        else {
                            Log.d("Parking update :: ", "False : " + slot.getSlotId());
                        }
                    }
                });
    }

    @Override
    public void payment(UserHistory history, AppUser user) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        //Update History document
        firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                .collection("HISTORY").document(history.getTransactionId())
                .set(history)
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task task) {
                        if(task.isSuccessful()){
                            //Update Parking slot document
                            firestore.collection("PARKING").document(user.getSlotNumber())
                                    .update("booked", false,
                                            "transactionId", "",
                                            "userId", "",
                                            "parked", false,
                                            "licensePlate", "",
                                            "elapsed_timer", false)
                                    .addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull @NotNull Task task) {
                                            if(task.isSuccessful()){
                                                //Update User document
                                                user.setSlotNumber(""); //j
                                                user.setWallet(user.getWallet() - history.getAmount());
                                                user.setTransactionId(""); //j
                                                user.setIrrelevant(0);
                                                firestore.collection("USERS").document(firebaseAuth.getCurrentUser().getUid())
                                                        .update("slotNumber", user.getSlotNumber(),
                                                                "wallet", user.getWallet(),
                                                                "transactionId", user.getTransactionId(),
                                                                "isReady",false,
                                                                "parked",false,
                                                                "isCancel", false,
                                                                "irrelevant", user.getIrrelevant())
                                                        .addOnCompleteListener(new OnCompleteListener() {
                                                            @Override
                                                            public void onComplete(@NonNull @NotNull Task task) {
                                                                if(task.isSuccessful()){

                                                                    Toast.makeText(MainActivity.this,
                                                                            "Thank you!! Visit again " + user.getName() + "!! :)",
                                                                            Toast.LENGTH_SHORT).show();
                                                                    Log.d("Successfully Checkout",
                                                                            "Please visit again" + user.getName());

                                                                    selectorFragment = new BookingFragment();
                                                                    getSupportFragmentManager().beginTransaction()
                                                                            .replace(R.id.fragment_container, selectorFragment)
                                                                            .commit();
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

}