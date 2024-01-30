package ru.netology.netologyyamaps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import ru.netology.netologyyamaps.R
import ru.netology.netologyyamaps.adapter.PlacesAdapter
import ru.netology.netologyyamaps.databinding.PlacesFragmentBinding
import ru.netology.netologyyamaps.dto.Place
import ru.netology.netologyyamaps.viewmodel.MapViewModel

class PlacesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = PlacesFragmentBinding.inflate(inflater, container, false)
        val viewModel by viewModels<MapViewModel>()
        val adapter = PlacesAdapter(object : PlacesAdapter.Listener {

            override fun onClick(place: Place) {
                findNavController().navigate(
                    R.id.action_placesFragment_to_mapFragment, bundleOf(
                        MapFragment.LAT_KEY to place.lat,
                        MapFragment.LONG_KEY to place.long
                    )
                )
            }

            override fun onDelete(place: Place) {
                viewModel.deletePlaceById(place.id)
            }

            override fun onEdit(place: Place) {
                findNavController().navigate( //запуск диалога по навигэйт
                    R.id.addPlace, AddPlaceDialog.createBundle(
                        place.lat, place.long, place.id
                    )
                )
            }
        })
        binding.listOfPlaces.adapter = adapter

        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenCreated {
            viewModel.places.collectLatest { places ->
                adapter.submitList(places)
                binding.empty.isVisible = places.isEmpty()
            }
        }
        return binding.root
    }

}

