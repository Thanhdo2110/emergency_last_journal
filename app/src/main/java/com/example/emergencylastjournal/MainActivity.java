package com.example.emergencylastjournal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Dùng SharedPreferences để đọc ngôn ngữ nhanh hơn, tránh crash do Room chạy trên UI thread
        SharedPreferences prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String language = prefs.getString("language", "Vietnam");
        
        String langCode = language.equalsIgnoreCase("English") ? "en" : "vi";
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
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
                } else if (destId == R.id.navigation_map) {
                    appBarLayout.setVisibility(View.GONE);
                    bottomNav.setVisibility(View.VISIBLE);
                } else {
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
