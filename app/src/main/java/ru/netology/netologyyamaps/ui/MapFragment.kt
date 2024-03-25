package ru.netology.netologyyamaps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.ui_view.ViewProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.netologyyamaps.R
import ru.netology.netologyyamaps.databinding.MapFragmentBinding
import ru.netology.netologyyamaps.databinding.PlaceBinding
import ru.netology.netologyyamaps.viewmodel.MapViewModel

class MapFragment : Fragment() {

    companion object {
        const val LAT_KEY = "LAT_KEY"
        const val LONG_KEY = "LONG_KEY"
    }

    private var mapView: MapView? = null
    private lateinit var userLocation: UserLocationLayer
    private var listener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) = Unit

        override fun onMapLongTap(map: Map, point: Point) {
            findNavController().navigate( //запуск диалога по навигэйт
                R.id.addPlace, AddPlaceDialog.createBundle(
                    point.latitude, point.longitude
                )
            )
//            AddPlaceDialog.newInstance(point.latitude, point.longitude) //просто запуск диалога
//                .show(childFragmentManager, null)
        }
    }

    private val locationObjectListener = object : UserLocationObjectListener {
        override fun onObjectAdded(view: UserLocationView) = Unit

        override fun onObjectRemoved(view: UserLocationView) = Unit

        override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {
            userLocation.cameraPosition()?.target?.let {
                mapView?.mapWindow?.map?.move(CameraPosition(it, 10F, 0F, 0F))
            }
            userLocation.setObjectListener(null)
        }
    }

    private val viewModel by viewModels<MapViewModel>()

    private val placeTapListener = MapObjectTapListener { mapObject, _ ->
        viewModel.deletePlaceById(mapObject.userData as Long)
        true
    }

    private val premissionLauncher = //запрос на разрешение на геолокацию
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            when {
                granted -> {
                    MapKitFactory.getInstance().resetLocationManagerToDefault()
                    userLocation.cameraPosition()?.target?.also {
                        val map = mapView?.mapWindow?.map ?: return@registerForActivityResult
                        val cameraPosition = map.cameraPosition
                        map.move(
                            CameraPosition(
                                it,
                                cameraPosition.zoom,
                                cameraPosition.azimuth,
                                cameraPosition.tilt,
                            )
                        )
                    }
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        "Location permission required",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = MapFragmentBinding.inflate(inflater, container, false)

        mapView = binding.map.apply {
            userLocation = MapKitFactory.getInstance().createUserLocationLayer(mapWindow)
            userLocation.isVisible = true
            userLocation.isHeadingEnabled = false
            mapWindow.map.addInputListener(listener)

            val collection = mapWindow.map.mapObjects.addCollection()
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.places.collectLatest { places ->
                        collection.clear()
                        places.forEach { place -> //делаем маркеру вьюшку
                            val placeBinding = PlaceBinding.inflate(layoutInflater)
                            placeBinding.title.text = place.name
                            collection.addPlacemark(
                                Point(place.lat, place.long),
                                ViewProvider(placeBinding.root)
                            ).apply {
                                userData = place.id
                            }
                        }
                    }
                }
            }
            collection.addTapListener(placeTapListener)

            //переход к точке на карте после клика на списке
            val arguments = arguments
            if (arguments != null &&
                arguments.containsKey(LAT_KEY) &&
                arguments.containsKey(LONG_KEY)
            ) {
                val cameraPosition = mapWindow.map.cameraPosition
                mapWindow.map.move(
                    CameraPosition(
                        Point(arguments.getDouble(LAT_KEY), arguments.getDouble(LONG_KEY)),
                        10F,
                        cameraPosition.azimuth,
                        cameraPosition.tilt
                    )
                )
                arguments.remove(LAT_KEY)
                arguments.remove(LONG_KEY)
            } else {
                //При входе в приложение показываем текущее местоположение
                userLocation.setObjectListener(locationObjectListener)
            }
        }

        binding.plus.setOnClickListener {
            binding.map.mapWindow.map.move(
                CameraPosition(
                    binding.map.mapWindow.map.cameraPosition.target,
                    binding.map.mapWindow.map.cameraPosition.zoom + 1,
                    0.0f,
                    0.0f
                ),
                Animation(Animation.Type.SMOOTH, 0.3F),
                null
            )
        }

        binding.minus.setOnClickListener {
            binding.map.mapWindow.map.move(
                CameraPosition(
                    binding.map.mapWindow.map.cameraPosition.target,
                    binding.map.mapWindow.map.cameraPosition.zoom - 1,
                    0.0f,
                    0.0f
                ),
                Animation(Animation.Type.SMOOTH, 0.3F),
                null
            )
        }

        binding.location.setOnClickListener {//разрешение на текущую локацию?
            premissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)

        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.map_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                if (menuItem.itemId == R.id.listOfPlaces) {
                    findNavController().navigate(R.id.action_mapFragment_to_placesFragment)
                    true
                } else {
                    false
                }
        }, viewLifecycleOwner)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        MapKitFactory.getInstance().onStart()

    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        MapKitFactory.getInstance().onStop()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView = null
    }
}