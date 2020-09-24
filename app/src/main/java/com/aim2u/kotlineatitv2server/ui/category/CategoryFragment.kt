package com.aim2u.kotlineatitv2server.ui.category

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aim2u.kotlineatitv2server.R
import com.aim2u.kotlineatitv2server.adapter.MyCategoriesAdapter
import com.aim2u.kotlineatitv2server.callback.IMyButtonCallback
import com.aim2u.kotlineatitv2server.common.Common
import com.aim2u.kotlineatitv2server.common.MySwipeHelper
import com.aim2u.kotlineatitv2server.model.CategoryModel
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.layout_update_category.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CategoryFragment : Fragment() {


    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter?=null
    private var recyclerCategory: RecyclerView?=null

    //33
    internal var categoryModels: List<CategoryModel> = ArrayList<CategoryModel>()
    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUri: Uri?=null
    internal lateinit var imgCategory : ImageView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        categoryViewModel =
            ViewModelProvider(this).get(CategoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_category, container, false)


        categoryViewModel.getMessageError().observe(viewLifecycleOwner,Observer{
            Toast.makeText(context,it, Toast.LENGTH_SHORT).show()
        })

        categoryViewModel.getCategoryList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            categoryModels = it
            adapter = MyCategoriesAdapter(requireContext(), categoryModels)
            recyclerCategory!!.adapter = adapter
            recyclerCategory!!.layoutAnimation = layoutAnimationController
        })

        initViews(root)
        return root
    }

    private fun initViews(root: View) {

        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        dialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        recyclerCategory = root.findViewById(R.id.recyler_menu) as RecyclerView
        recyclerCategory!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)

        recyclerCategory!!.layoutManager = layoutManager
        recyclerCategory!!.addItemDecoration(
            DividerItemDecoration(context,layoutManager.orientation)
        )

        val swipe = object : MySwipeHelper(requireContext(),recyclerCategory!!,200){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(requireContext(),
                    "Update",
                    30,
                    0,
                    Color.parseColor("#FF3c30"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {
                            Common.categorySelected = categoryModels[pos];

                            showUpdateDialog()
                        }
                    }))
            }
        }
    }

    private fun showUpdateDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        builder.setTitle("Update Category")
        builder.setMessage("Please Fill Information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)

        val edtCategoryName = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        imgCategory = itemView.findViewById<View>(R.id.img_category) as ImageView
        edtCategoryName.setText(Common.categorySelected?.name)
        Glide.with(requireContext()).load(Common.categorySelected?.image).into(imgCategory)

        imgCategory.setOnClickListener { view ->
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL"){dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("UPDATE"){dialogInterface, _ ->
            val updateData = HashMap<String,Any>()
            updateData["name"] = edtCategoryName.text.toString()
            if(imageUri != null){
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
                            updateData["image"] = uri.toString()
                            updateCategory(updateData)
                        }
                    }
            } else {
                updateCategory(updateData)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun updateCategory(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.categorySelected?.menu_id!!)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task ->
                categoryViewModel?.loadCategory()
                Toast.makeText(context,"Update Success",Toast.LENGTH_SHORT).show()
            }
    }
}