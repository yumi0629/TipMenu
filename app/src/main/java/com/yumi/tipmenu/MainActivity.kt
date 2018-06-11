package com.yumi.tipmenu

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.yumi.tipmenu.tipmenu.PopupMenuListener
import com.yumi.tipmenu.tipmenu.TipMenu
import java.util.ArrayList

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val menus = ArrayList<String>()
        menus.add("回复")
        menus.add("点赞")
        menus.add("复制")
        menus.add("举报")
        val tipMenu = TipMenu(this, clickType = TipMenu.TYPE_CLICK)
        tipMenu.bind(
                tv_test1,
                menus,
                popupMenuListener = object : PopupMenuListener {
                    override fun onPopupMenuClick(position: Int) {
                        Toast.makeText(this@MainActivity, "${menus[position]} : $position", Toast.LENGTH_SHORT).show()
                    }

                }
        )

        val tipMenu2 = TipMenu(this, clickType = TipMenu.TYPE_CLICK, cornerPosition = TipMenu.CORNER_POSITION_BOTTOM)
        tipMenu2.bind(
                tv_test2,
                menus,
                popupMenuListener = object : PopupMenuListener {
                    override fun onPopupMenuClick(position: Int) {
                        Toast.makeText(this@MainActivity, "${menus[position]} : $position", Toast.LENGTH_SHORT).show()
                    }

                }
        )
    }
}
