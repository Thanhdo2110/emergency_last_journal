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
            
            // Thiết lập mặc định của NavigationUI
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Tùy chỉnh xử lý click để luôn quay về Home "sạch" nhất
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                // Nếu nhấn vào nút Home
                if (itemId == R.id.navigation_home) {
                    // Kiểm tra nếu không phải đang ở Home thì mới thực hiện chuyển
                    if (navController.getCurrentDestination() != null && 
                        navController.getCurrentDestination().getId() != R.id.navigation_home) {
                        
                        navController.navigate(itemId, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true) // Xóa sạch backstack tới tận gốc
                                .setLaunchSingleTop(true)
                                .build());
                    }
                    return true;
                }
                
                // Các tab khác sử dụng logic chuyển hướng mặc định của thư viện
                return NavigationUI.onNavDestinationSelected(item, navController);
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                // Tự động ẩn toolbar ở map, hiện ở các màn khác
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