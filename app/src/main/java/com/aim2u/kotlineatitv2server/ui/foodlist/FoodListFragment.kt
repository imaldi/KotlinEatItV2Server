package com.aim2u.kotlineatitv2server.ui.foodlist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2server.R
import com.aim2u.kotlineatitv2server.adapter.MyFoodListAdapter
import com.aim2u.kotlineatitv2server.callback.IMyButtonCallback
import com.aim2u.kotlineatitv2server.common.Common
import com.aim2u.kotlineatitv2server.common.MySwipeHelper
import com.aim2u.kotlineatitv2server.eventbus.MenuItemBack
import com.aim2u.kotlineatitv2server.model.FoodModel
import com.google.firebase.database.FirebaseDatabase
import org.greenrobot.eventbus.EventBus

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel

    var recyler_food_list : RecyclerView?=null
    var layoutAnimationController : LayoutAnimationController?=null

    var adapter : MyFoodListAdapter?=null
    var foodModelList : List<FoodModel> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel =
            ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)
        initViews(root)
        foodListViewModel.getMutableFoodModelListData().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                foodModelList = it
                adapter = MyFoodListAdapter(requireContext(), foodModelList)
                recyler_food_list!!.adapter = adapter
                recyler_food_list!!.layoutAnimation = layoutAnimationController
            }
        })
        return root
    }

    private fun initViews(root: View?) {
        recyler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recyler_food_list!!.setHasFixedSize(true)
        recyler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        (activity as AppCompatActivity).supportActionBar?.title = Common.categorySelected?.name

        val swipe = object : MySwipeHelper(requireContext(),recyler_food_list!!,200){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(requireContext(),
                    "Delete",
                    30,
                    0,
                    Color.parseColor("#9b0000"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {
                            Common.foodSelected = foodModelList[pos]
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setTitle("Delete")
                                .setMessage("Do you really want to delete food?")
                                .setNegativeButton("CANCEL"){dialogInterface,_ ->
                                    dialogInterface.dismiss()
                                }
                                .setPositiveButton("DELETE"){ dialogInterface, _ ->
                                    Common.categorySelected?.foods?.removeAt(pos)
                                    updateFood(Common.categorySelected?.foods)
                                }

                            val deleteDialog = builder.create()
                            deleteDialog.show()
                        }
                    }))
                buffer.add(MyButton(requireContext(),
                    "Update",
                    30,
                    0,
                    Color.parseColor("#9b9b00"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {

                        }
                    }))
            }
        }
    }

    private fun updateFood(foods: MutableList<FoodModel>?) {
        val updateData = HashMap<String,Any>()
        updateData["foods"] = foods!!

        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.categorySelected?.menu_id!!)
            .updateChildren(updateData)
            .addOnFailureListener { e -> Toast.makeText(requireContext(),""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    foodListViewModel.getMutableFoodModelListData()
                    Toast.makeText(requireContext(), "Delete Success", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}