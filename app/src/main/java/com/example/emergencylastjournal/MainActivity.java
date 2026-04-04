package com.example.emergencylastjournal;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
            NavigationUI.setupWithNavController(bottomNav, navController);

            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    if (navController.getCurrentDestination() != null && 
                        navController.getCurrentDestination().getId() != R.id.navigation_home) {
                        
                        navController.navigate(itemId, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .setLaunchSingleTop(true)
                                .build());
                    }
                    return true;
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });
        }

        // TỐI ƯU INSETS: Xử lý cả Top và Bottom một cách chính xác
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Padding top cho main container để tránh status bar
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            // Margin bottom cho BottomNavigationView để nó không bị Navigation Bar của hệ thống che
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                layoutParams.bottomMargin = systemBars.bottom;
                bottomNav.setLayoutParams(layoutParams);
            }
            
            return WindowInsetsCompat.CONSUMED;
        });

        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                
                if (destId == R.id.navigation_welcome) {
                    appBarLayout.setVisibility(View.GONE);
                    bottomNav.setVisibility(View.GONE);
                } 
                else if (destId == R.id.navigation_map) {
                    appBarLayout.setVisibility(View.GONE);
                    bottomNav.setVisibility(View.VISIBLE);
                } 
                else {
                    appBarLayout.setVisibility(View.VISIBLE);
                    bottomNav.setVisibility(View.VISIBLE);
                    toolbar.setTitle(destination.getLabel());
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp()) || super.onSupportNavigateUp();
    }
}
