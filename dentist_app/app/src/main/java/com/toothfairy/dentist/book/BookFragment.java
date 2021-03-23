package com.toothfairy.dentist.book;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.toothfairy.dentist.*;
import com.toothfairy.dentist.R;
import com.toothfairy.dentist.intro.IntroFragment;

import java.util.Calendar;
import java.util.Objects;

public class BookFragment extends Fragment {
    public static BookFragment newInstance() {
        return new BookFragment();
    }

    FirebaseUser user;
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mDatabaseReference;
    private ListView listView;
    private ListViewAdapter adapter;
    Dialog dialog;

    String[] weekDay = {"일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"};
    int y, m, d, h;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_book, container, false);
        user = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        RadioButton etc = root.findViewById(R.id.etc);

        root.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).replaceFragment(IntroFragment.newInstance());
            }
        });
        etc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getView().findViewById(R.id.etcEdit).setVisibility(View.VISIBLE);
            }
        });
        final TextView detailEdit = root.findViewById(R.id.detailEdit);
        dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_book);

        adapter = new ListViewAdapter();

        final TextView time = dialog.findViewById(R.id.time);

        listView = dialog.findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("정말로 예약하시겠습니까?");
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
                final ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);

                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int which) {
                        //PatientID patientID = new PatientID(); // 로그인 된 사용자의 정보를 가져온다.
                        mFirebaseDatabase.getReference("patient/" + user.getUid()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull final DataSnapshot snapshot) {
                                PatientID patient = snapshot.getValue(PatientID.class);
                                BookInfo bookInfo = new BookInfo();
                                bookInfo.setName(patient.getName());
                                bookInfo.setTime(item.getTime());
                                bookInfo.setPhoneNum(patient.getPhoneNum());
                                bookInfo.setSubject(getCheckbox());
                                bookInfo.setDetail(detailEdit.getText().toString());
                                mFirebaseDatabase.getReference("book/" + y + "/" + m + "/" + d)// + item.getTime() + "/")
                                        .push()
                                        .setValue(bookInfo)
                                        .addOnSuccessListener(Objects.requireNonNull(getActivity()), new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                dialog.dismiss();
                                                adapter.clear();
                                                Toast.makeText(getActivity(), "예약이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                                setYesterdayAlarm(m, d, item.getTime());
                                                setOneHoursAgoAlarm(y, m, d, item.getTime());
                                                ((MainActivity) getActivity()).replaceFragment(IntroFragment.newInstance());
                                            }
                                        });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });

                    }
                });
                builder.create().show();
            }

        });
        final CalendarView calendarView = root.findViewById(R.id.calendar);
        final Calendar calendar = Calendar.getInstance();
        long startOfMonth = calendar.getTimeInMillis();
        calendarView.setMinDate(startOfMonth);
        calendarView.setMaxDate(startOfMonth + 1000L * 60 * 60 * 24 * 30);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView calendarView, int year, int month, int dayOfMonth) {
                Calendar subCalendar = Calendar.getInstance();
                subCalendar.set(year, month, dayOfMonth);
                int dayOfWeek = subCalendar.get(Calendar.DAY_OF_WEEK) - 1;
                if (dayOfWeek != 0 && dayOfWeek != 6)
                    showDialog(year, month, dayOfMonth, dayOfWeek);
            }
        });
        return root;
    }

    private void setYesterdayAlarm(int m, int d, int hour) {
        PackageManager pm = getActivity().getPackageManager();
        ComponentName receiver = new ComponentName(getActivity(), DeviceBootReceiver.class);
        Intent alarmIntent = new Intent(getActivity(), AlarmReceiver.class);
        alarmIntent.putExtra("yesterd_m",m);
        alarmIntent.putExtra("yesterd_d",d);
        alarmIntent.putExtra("yesterd_h",hour);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, y);
        calendar.set(Calendar.MONTH, m-1);
        calendar.set(Calendar.DATE, d - 1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 4);
        calendar.set(Calendar.SECOND, 0);
        //SharedPreferences.Editor editor = getActivity().getSharedPreferences("daily alarm", MODE_PRIVATE).edit();
        if (alarmManager != null) {
            //alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private void setOneHoursAgoAlarm(int y, int m, int d, int hour) {
    }

    private String getCheckbox() {
        RadioGroup rdGroup = getView().findViewById(R.id.rdGroup);
        int subjectID = rdGroup.getCheckedRadioButtonId();

        if (subjectID == 0x7f0900b1) {
            EditText etcEdit = getView().findViewById(R.id.etcEdit);
            return etcEdit.getText().toString();
        } else {
            RadioButton rb = getView().findViewById(subjectID);
            return rb.getText().toString();
        }

    }

    public void showDialog(int year, int month, int dayOfMonth, int dayOfWeek) { //월, 연
        adapter.clear();
        dialog.show();
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        dialogTitle.setText((month + 1) + "월" + dayOfMonth + "일" + weekDay[dayOfWeek]);
        y = year;
        m = month + 1;
        d = dayOfMonth;
        String ref = "Book/" + year + "/" + month + "/" + dayOfMonth + "/";

        int max = 17;
        if (dayOfWeek == 2 || dayOfWeek == 4) // 화요일이나 목요일일 때
            max = 20;

        for (int i = 9; i <= max; i++) {
            if (i == 12)
                continue;
            adapter.addItem(ref, i);
        }
        adapter.notifyDataSetChanged();

        listView.setAdapter(adapter);

        (dialog.findViewById(R.id.cancelBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}