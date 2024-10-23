import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mitsubishi.R
import com.example.mitsubishi.car

class CarListAdapter(private val itemList: List<car>, private val itemClickListener: OnItemClickListener) : RecyclerView.Adapter<CarListAdapter.ItemViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(car: car)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noPol: TextView = itemView.findViewById(R.id.no_polisi)
        val status: TextView = itemView.findViewById(R.id.detail)

        fun bind(car: car, clickListener: OnItemClickListener) {
            noPol.text = car.nopol
            status.text = car.status

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.costomer_list, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = itemList[position]
        holder.bind(currentItem, itemClickListener) // Bind the current item to the holder
    }

    override fun getItemCount() = itemList.size
}
