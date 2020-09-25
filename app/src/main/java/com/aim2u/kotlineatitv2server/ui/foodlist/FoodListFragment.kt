package com.aim2u.kotlineatitv2server.ui.foodlist

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2server.R
import com.aim2u.kotlineatitv2server.SizeAddonEditActivity
import com.aim2u.kotlineatitv2server.adapter.MyFoodListAdapter
import com.aim2u.kotlineatitv2server.callback.IMyButtonCallback
import com.aim2u.kotlineatitv2server.common.Common
import com.aim2u.kotlineatitv2server.common.MySwipeHelper
import com.aim2u.kotlineatitv2server.eventbus.AddonSizeEditEvent
import com.aim2u.kotlineatitv2server.eventbus.ChangeMenuClick
import com.aim2u.kotlineatitv2server.eventbus.MenuItemBack
import com.aim2u.kotlineatitv2server.eventbus.ToastEvent
import com.aim2u.kotlineatitv2server.model.FoodModel
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.layout_update_food.*
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FoodListFragment : Fragment() {

    private lateinit var img_food: ImageView
    private var imageUri: Uri?=null
    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var foodListViewModel: FoodListViewModel

    var recyler_food_list : RecyclerView?=null
    var layoutAnimationController : LayoutAnimationController?=null

    var adapter : MyFoodListAdapter?=null
    var foodModelList : List<FoodModel> = ArrayList()

    //Variable
    private lateinit var storage : FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var dialog: AlertDialog


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

        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        recyler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recyler_food_list!!.setHasFixedSize(true)
        recyler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        (activity as AppCompatActivity).supportActionBar?.title = Common.categorySelected?.name

        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val swipe = object : MySwipeHelper(requireContext(),recyler_food_list!!,width/6){
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
                                    updateFood(Common.categorySelected?.foods, true)
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
//                            Common.foodSelected = foodModelList[pos]
                            showUpdateDialog(pos)
                        }
                    }))

                //Size and addon edit
                buffer.add(MyButton(requireContext(),
                    "Size",
                    30,
                    0,
                    Color.parseColor("#12005e"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {
                            Common.foodSelected = foodModelList[pos]
                            startActivity(Intent(requireContext(),SizeAddonEditActivity::class.java))
                            EventBus.getDefault().postSticky(AddonSizeEditEvent(false,pos))
                        }
                    }))

                buffer.add(MyButton(requireContext(),
                    "Addon",
                    30,
                    0,
                    Color.parseColor("#333639"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {
                            Common.foodSelected = foodModelList[pos]
                            startActivity(Intent(requireContext(),SizeAddonEditActivity::class.java))
                            EventBus.getDefault().postSticky(AddonSizeEditEvent(true,pos))
                        }
                    }))
            }
        }
    }

    private fun showUpdateDialog(pos: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update")
            .setMessage("Please Fill Information")

        val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_update_food,null)

        val edtFoodName = itemView.findViewById<View>(R.id.edt_food_name) as EditText
        val edtFoodPrice = itemView.findViewById<View>(R.id.edt_food_price) as EditText
        val edtFoodDescription = itemView.findViewById<View>(R.id.edt_food_description) as EditText
        img_food = itemView.findViewById<View>(R.id.img_food_dialog) as ImageView

        //Set Data
        edtFoodName.setText(StringBuilder("").append(Common.categorySelected?.foods!![pos].name))
        edtFoodPrice.setText(StringBuilder("").append(Common.categorySelected?.foods!![pos].price))
        edtFoodDescription.setText(StringBuilder("").append(Common.categorySelected?.foods!![pos].price))
        Glide.with(requireContext()).load(Common.categorySelected?.foods!![pos].image).into(img_food)

        //Set Event
        img_food.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL"){ dialogInterface, _ -> dialogInterface.dismiss()}
        builder.setPositiveButton("UPDATE"){dialogInterface, _ ->
            val updateFood = Common.categorySelected?.foods!![pos]
            updateFood.name = edtFoodName.text.toString()
            updateFood.price = if(TextUtils.isEmpty(edtFoodPrice.text)){
                0
            } else {
                edtFoodPrice.text.toString().toLong()
            }

            updateFood.description = edtFoodDescription.text.toString()

            if (imageUri != null){
                dialog.setMessage("Uploading...")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener{e ->
                        dialog.dismiss()
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount

                        dialog.setMessage("Upload $progress%")
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener {uri ->
                            updateFood.image = uri.toString()
                            Common.categorySelected?.foods!![pos] = updateFood
                            updateFood(Common.categorySelected?.foods!!,false)
                        }
                    }
            }else{
                Common.categorySelected?.foods!![pos] = updateFood
                updateFood(Common.categorySelected?.foods!!,false)
            }
        }
        builder.setView(itemView)

        val updateDialog = builder.create()
        updateDialog.show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if(data != null && data.data != null){
                imageUri = data.data
                img_food.setImageURI(imageUri)
            }
        }
    }

    private fun updateFood(foods: MutableList<FoodModel>?, isDeleted: Boolean) {
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
                    EventBus.getDefault().postSticky(ToastEvent(!isDeleted,true))
                }
            }
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }
}