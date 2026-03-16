package com.example.emergencylastjournal;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.emergencylastjournal.data.db.AppDatabase;
import com.example.emergencylastjournal.data.entity.ContactEntity;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigation);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Kết nối chuẩn của Google
            NavigationUI.setupWithNavController(bottomNav, navController);

            // FIX LOGIC: Xử lý khi nhấn vào Tab điều hướng
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                // Nếu nhấn vào icon, xóa hết các màn hình con để về gốc của Tab đó
                navController.popBackStack(itemId, false);
                
                // Thực hiện chuyển Tab mượt mà
                NavigationUI.onNavDestinationSelected(item, navController);
                return true;
            });

            // Xử lý khi nhấn lại vào chính Tab đang chọn (Reselect)
            bottomNav.setOnItemReselectedListener(item -> {
                navController.popBackStack(item.getItemId(), false);
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        findViewById(R.id.fabSos).setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.navigation_map);
            }
        });

        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.navigation_map) {
                    appBarLayout.setVisibility(View.GONE);
                } else {
                    appBarLayout.setVisibility(View.VISIBLE);
                    toolbar.setTitle(destination.getLabel());
                }
            });
        }

        createMockData();
    }

    private void createMockData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (db.contactDao().getAllContactsSync().isEmpty()) {
                ContactEntity mockContact = new ContactEntity();
                mockContact.name = "Người thân mẫu";
                mockContact.phone = "0901234567";
                mockContact.shareLocation = true;
                mockContact.verified = true;
                db.contactDao().insert(mockContact);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}