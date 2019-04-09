package com.softwarefactory.chat.ynl

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.softwarefactory.chat.ynl.data.StaticConfig
import com.softwarefactory.chat.ynl.service.ServiceUtils
import com.softwarefactory.chat.ynl.ui.FriendsFragment
import com.softwarefactory.chat.ynl.ui.GroupFragment
import com.softwarefactory.chat.ynl.ui.LoginActivity
import com.softwarefactory.chat.ynl.ui.UserProfileFragment

import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    private var viewPager: ViewPager? = null
    private var tabLayout: TabLayout? = null

    private var floatButton: FloatingActionButton? = null
    private var adapter: ViewPagerAdapter? = null

    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }

        viewPager = findViewById<View>(R.id.viewpager) as ViewPager
        floatButton = findViewById<View>(R.id.fab) as FloatingActionButton
        initTab()
        initFirebase()
    }

    private fun initFirebase() {
        //Khoi tao thanh phan de dang nhap, dang ky
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
            if (user != null) {
                StaticConfig.UID = user!!.uid
            } else {
                this@MainActivity.finish()
                // User is signed in
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                Log.d(TAG, "onAuthStateChanged:signed_out")
            }
            // ...
        }
    }


    override fun onStart() {
        super.onStart()
        mAuth!!.addAuthStateListener(mAuthListener!!)
        ServiceUtils.stopServiceFriendChat(applicationContext, false)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onDestroy() {
        ServiceUtils.startServiceFriendChat(applicationContext)
        super.onDestroy()
    }

    /**
     * Khoi tao 3 tab
     */
    private fun initTab() {
        tabLayout = findViewById<View>(R.id.tabs) as TabLayout
        tabLayout!!.setSelectedTabIndicatorColor(resources.getColor(R.color.colorIndivateTab))
        setupViewPager(viewPager!!)
        tabLayout!!.setupWithViewPager(viewPager)
        setupTabIcons()
    }


    private fun setupTabIcons() {
        val tabIcons = intArrayOf(R.drawable.ic_tab_person, R.drawable.ic_tab_group, R.drawable.ic_tab_infor)

        tabLayout!!.getTabAt(0)!!.setIcon(tabIcons[0])
        tabLayout!!.getTabAt(1)!!.setIcon(tabIcons[1])
        tabLayout!!.getTabAt(2)!!.setIcon(tabIcons[2])
    }

    private fun setupViewPager(viewPager: ViewPager) {
        adapter = ViewPagerAdapter(supportFragmentManager)
        adapter!!.addFrag(FriendsFragment(), STR_FRIEND_FRAGMENT)
        adapter!!.addFrag(GroupFragment(), STR_GROUP_FRAGMENT)
        adapter!!.addFrag(UserProfileFragment(), STR_INFO_FRAGMENT)
        floatButton!!.setOnClickListener((adapter!!.getItem(0) as FriendsFragment).onClickFloatButton.getInstance(this))
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 3
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            @SuppressLint("RestrictedApi")
            override fun onPageSelected(position: Int) {
                ServiceUtils.stopServiceFriendChat(this@MainActivity.applicationContext, false)
                if (adapter!!.getItem(position) is FriendsFragment) {
                    floatButton!!.visibility = View.VISIBLE
                    floatButton!!.setOnClickListener(
                        (adapter!!.getItem(position) as FriendsFragment).onClickFloatButton.getInstance(
                            this@MainActivity
                        )
                    )
                    floatButton!!.setImageResource(R.drawable.plus)
                } else if (adapter!!.getItem(position) is GroupFragment) {
                    floatButton!!.visibility = View.VISIBLE
                    floatButton!!.setOnClickListener(
                        (adapter!!.getItem(position) as GroupFragment).onClickFloatButton.getInstance(
                            this@MainActivity
                        )
                    )
                    floatButton!!.setImageResource(R.drawable.ic_float_add_group)
                } else {
                    floatButton!!.visibility = View.GONE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    //    @Override
    //    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //        super.onActivityResult(requestCode, resultCode, data);
    //        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
    //            if (data.getStringExtra(STR_EXTRA_ACTION).equals(LoginActivity.STR_EXTRA_ACTION_LOGIN)) {
    //                authUtils.signIn(data.getStringExtra(STR_EXTRA_USERNAME), data.getStringExtra(STR_EXTRA_PASSWORD));
    //            } else if (data.getStringExtra(STR_EXTRA_ACTION).equals(RegisterActivity.STR_EXTRA_ACTION_REGISTER)) {
    //                authUtils.createUser(data.getStringExtra(STR_EXTRA_USERNAME), data.getStringExtra(STR_EXTRA_PASSWORD));
    //            }else if(data.getStringExtra(STR_EXTRA_ACTION).equals(LoginActivity.STR_EXTRA_ACTION_RESET)){
    //                authUtils.resetPassword(data.getStringExtra(STR_EXTRA_USERNAME));
    //            }
    //        } else if (resultCode == RESULT_CANCELED) {
    //            this.finish();
    //        }
    //    }
    //    @Override
    //    public boolean onCreateOptionsMenu(Menu menu) {
    //        // Inflate the menu; this adds items to the action bar if it is present.
    //        getMenuInflater().inflate(R.menu.menu_main, menu);
    //        return true;
    //    }
    //
    //    @Override
    //    public boolean onOptionsItemSelected(MenuItem item) {
    //        // Handle action bar item clicks here. The action bar will
    //        // automatically handle clicks on the Home/Up button, so long
    //        // as you specify a parent activity in AndroidManifest.xml.
    //        int id = item.getItemId();
    //
    //        //noinspection SimplifiableIfStatement
    //        if (id == R.id.about) {
    //            Toast.makeText(this, "Rivchat version 1.0", Toast.LENGTH_LONG).show();
    //            return true;
    //        }
    //
    //        return super.onOptionsItemSelected(item);
    //    }

    /**
     * Adapter hien thi tab
     */
    internal inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList = ArrayList<Fragment>()
        private val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {

            // return null to display only the icon
            return null
        }
    }

    companion object {
        private val TAG = "MainActivity"
        var STR_FRIEND_FRAGMENT = "FRIEND"
        var STR_GROUP_FRAGMENT = "GROUP"
        var STR_INFO_FRAGMENT = "INFO"
    }
}