package com.abhishek.parkingsystemapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abhishek.parkingsystemapp.Models.UserHistory;
import com.abhishek.parkingsystemapp.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder>{

    Context context;
    List<UserHistory> historyList;

    public HistoryAdapter(Context context, List<UserHistory> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_list, parent, false);
        return new HistoryAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull HistoryAdapter.ViewHolder holder, int position) {

        UserHistory history = historyList.get(position);

        String bookTime = history.getBookingTime().toDate().toString();
        bookTime = bookTime.substring(0, bookTime.length() - 14) + bookTime.substring(bookTime.length() - 4);
        String arrival = "---";
        if(history.getArrival() != null){
            arrival = history.getArrival().toDate().toString();
            arrival = arrival.substring(0, arrival.length() - 14) + arrival.substring(arrival.length() - 4);

            if(history.getArrival().getSeconds() < history.getBookingTime().getSeconds()){
                holder.rlMain.setBackground(context.getResources().getDrawable(R.drawable.edit_text_background_cancelled));
                holder.tvArrival.setVisibility(View.GONE);
                holder.tvExit.setVisibility(View.GONE);
                holder.tvCancelled.setVisibility(View.VISIBLE);
                holder.tvArrivalText.setVisibility(View.GONE);
                holder.tvExitText.setVisibility(View.GONE);
            }
            else{
                holder.rlMain.setBackground(context.getResources().getDrawable(R.drawable.edit_text_background));
                holder.tvArrival.setVisibility(View.VISIBLE);
                holder.tvExit.setVisibility(View.VISIBLE);
                holder.tvCancelled.setVisibility(View.GONE);
                holder.tvArrivalText.setVisibility(View.VISIBLE);
                holder.tvExitText.setVisibility(View.VISIBLE);
            }
        }
        else{
            arrival = "---";
        }
//        arrival = history.getArrival().toDate().toString();
//        arrival = arrival.substring(0, arrival.length() - 14) + arrival.substring(arrival.length() - 4);
        String exit = "---";
        if (history.getExit() != null){
            exit = history.getExit().toDate().toString();
            exit = exit.substring(0, exit.length() - 14) + exit.substring(exit.length() - 4);
        }else{
            exit = "---";
        }
        holder.tvBooking.setText(bookTime);
        holder.tvArrival.setText(arrival);
        holder.tvExit.setText(exit);
        holder.tvAmount.setText("₹".concat(String.valueOf(history.getAmount())));
        holder.tvTransaction.setText(history.getTransactionId());

    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView tvBooking, tvArrival, tvExit, tvAmount, tvTransaction;
        TextView tvArrivalText, tvExitText;
        RelativeLayout rlMain;
        TextView tvCancelled;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            tvBooking = itemView.findViewById(R.id.tvBooking_historyList);
            tvArrival = itemView.findViewById(R.id.tvArrival_historyList);
            tvExit = itemView.findViewById(R.id.tvCheckout_historyList);
            tvAmount = itemView.findViewById(R.id.tvAmount_historyList);
            tvTransaction = itemView.findViewById(R.id.tvTransaction_historyList);
            rlMain = itemView.findViewById(R.id.rlhistoryList_main);
            tvCancelled = itemView.findViewById(R.id.tvCancelled);
            tvArrivalText = itemView.findViewById(R.id.tvArrival_historyList_Text);
            tvExitText = itemView.findViewById(R.id.tvCheckout_historyList_Text);

        }
    }

}
