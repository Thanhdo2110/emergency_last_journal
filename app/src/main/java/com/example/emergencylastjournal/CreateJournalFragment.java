package com.example.emergencylastjournal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.emergencylastjournal.data.entity.SessionEntity;
import com.example.emergencylastjournal.data.repository.SessionRepository;
import com.example.emergencylastjournal.service.TrackingForegroundService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateJournalFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "CreateJournalFragment";
    private SessionRepository repository;
    private TextInputEditText etRoute;
    private NumberPicker npHour, npMinute;
    private TextView tvTimerDisplay;
    private View rootView;
    private MaterialButtonToggleGroup toggleStatusGroup;
    
    private ImageView ivCapturedPhoto;
    private TextView tvLocationCoords, tvLocationTitle;
    private MapView liteMapView;
    private GoogleMap liteGoogleMap;
    private String currentPhotoPath;
    private Double currentLat, currentLng;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean locationGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (locationGranted) {
                    executeLocationLogic();
                } else {
                    Toast.makeText(getContext(), "Cần quyền vị trí để tiếp tục!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && currentPhotoPath != null) {
                    Glide.with(this).load(new File(currentPhotoPath)).into(ivCapturedPhoto);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_create_journal, container, false);
        liteMapView = rootView.findViewById(R.id.liteMapView);
        liteMapView.onCreate(savedInstanceState);
        liteMapView.getMapAsync(this);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new SessionRepository(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        etRoute = view.findViewById(R.id.etRouteDetail);
        tvTimerDisplay = view.findViewById(R.id.tvTimerDisplay);
        npHour = view.findViewById(R.id.npHour);
        npMinute = view.findViewById(R.id.npMinute);
        toggleStatusGroup = view.findViewById(R.id.toggleStatusGroup);
        ivCapturedPhoto = view.findViewById(R.id.ivCapturedPhoto);
        tvLocationCoords = view.findViewById(R.id.tvLocationCoords);
        tvLocationTitle = view.findViewById(R.id.tvLocationTitle);

        setupNumberPicker(npHour, 0, 23, 0);
        setupNumberPicker(npMinute, 0, 59, 15);

        NumberPicker.OnValueChangeListener timeChangeListener = (picker, oldVal, newVal) -> updateTimerDisplay();
        npHour.setOnValueChangedListener(timeChangeListener);
        npMinute.setOnValueChangedListener(timeChangeListener);

        view.findViewById(R.id.cardCapturePhoto).setOnClickListener(v -> checkCameraPermission());
        view.findViewById(R.id.cardGetLocation).setOnClickListener(v -> requestLocation());
        view.findViewById(R.id.btnSaveJournal).setOnClickListener(v -> checkPermissionsAndStart());
        
        // Tự động lấy vị trí khi vào màn hình
        requestLocation();
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        } else {
            executeLocationLogic();
        }
    }

    @SuppressLint("MissingPermission")
    private void executeLocationLogic() {
        tvLocationTitle.setText("Đang lấy vị trí...");
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    updateLocationUI(location);
                } else {
                    tvLocationTitle.setText("Không thể lấy vị trí hiện tại");
                    // Thử lấy vị trí cuối cùng nếu GPS không phản hồi
                    fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                        if (lastLoc != null) {
                            currentLat = lastLoc.getLatitude();
                            currentLng = lastLoc.getLongitude();
                            updateLocationUI(lastLoc);
                        }
                    });
                }
            })
            .addOnFailureListener(e -> {
                tvLocationTitle.setText("Lỗi định vị: " + e.getMessage());
            });
    }

    private void updateLocationUI(Location location) {
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        tvLocationTitle.setText("Vị trí của bạn");
        
        // Sử dụng Geocoder để lấy địa chỉ
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1, addresses -> {
                    if (getActivity() != null && !addresses.isEmpty()) {
                        getActivity().runOnUiThread(() -> tvLocationCoords.setText(addresses.get(0).getAddressLine(0)));
                    }
                });
            } else {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    tvLocationCoords.setText(addresses.get(0).getAddressLine(0));
                }
            }
        } catch (Exception e) {
            tvLocationCoords.setText(String.format(Locale.getDefault(), "Tọa độ: %.5f, %.5f", pos.latitude, pos.longitude));
        }

        if (liteGoogleMap != null) {
            liteMapView.setVisibility(View.VISIBLE);
            liteGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                liteGoogleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        liteGoogleMap = map;
        liteGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        if (currentLat != null && currentLng != null) {
            LatLng pos = new LatLng(currentLat, currentLng);
            liteGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = createImageFile();
            Uri photoURI = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureLauncher.launch(photoURI);
        } catch (IOException ex) {
            Toast.makeText(getContext(), "Lỗi camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void checkPermissionsAndStart() {
        executeStartProtection();
    }

    private void executeStartProtection() {
        String route = etRoute.getText().toString().trim();
        if (route.isEmpty()) {
            etRoute.setError("Vui lòng nhập lộ trình!");
            return;
        }

        int totalSeconds = (npHour.getValue() * 3600) + (npMinute.getValue() * 60);
        if (totalSeconds == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn thời gian bảo vệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        String status = "safe";
        int checkedId = toggleStatusGroup.getCheckedButtonId();
        if (checkedId == R.id.btnTiredState) status = "tired";
        else if (checkedId == R.id.btnDangerState) status = "danger";

        SessionEntity session = new SessionEntity();
        session.route = route;
        session.status = status;
        session.timerDuration = totalSeconds;
        session.startedAt = System.currentTimeMillis();
        session.photoPath = currentPhotoPath;
        session.latitude = currentLat;
        session.longitude = currentLng;
        session.outcome = "active";

        repository.insert(session, sessionId -> {
            Intent intent = new Intent(requireContext(), TrackingForegroundService.class);
            intent.putExtra("SESSION_ID", sessionId);
            intent.putExtra("DURATION_SECONDS", totalSeconds);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent);
            } else {
                requireContext().startService(intent);
            }
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "ĐÃ KÍCH HOẠT BẢO VỆ MỚI!", Toast.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_home);
                });
            }
        });
    }

    private void setupNumberPicker(NumberPicker picker, int min, int max, int value) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        picker.setWrapSelectorWheel(true);
    }

    private void updateTimerDisplay() {
        tvTimerDisplay.setText(String.format(Locale.getDefault(), "%02dg %02dp", npHour.getValue(), npMinute.getValue()));
    }

    @Override
    public void onResume() {
        super.onResume();
        liteMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        liteMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        liteMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        liteMapView.onLowMemory();
    }
}
