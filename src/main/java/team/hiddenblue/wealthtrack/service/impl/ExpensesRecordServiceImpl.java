package team.hiddenblue.wealthtrack.service.impl;

import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.hiddenblue.wealthtrack.constant.ResponseCode;
import team.hiddenblue.wealthtrack.dto.ExpensesRecordDto;
import team.hiddenblue.wealthtrack.pojo.ExpensesRecord;
import team.hiddenblue.wealthtrack.pojo.LedgerPermission;
import team.hiddenblue.wealthtrack.mapper.ExpensesRecordMapper;
import team.hiddenblue.wealthtrack.mapper.LedgerPermissionMapper;
import team.hiddenblue.wealthtrack.service.ExpensesRecordService;
import team.hiddenblue.wealthtrack.util.TimeUtil;

import java.text.ParseException;
import java.util.*;

@Service
public class ExpensesRecordServiceImpl implements ExpensesRecordService {
    @Autowired
    ExpensesRecordMapper expensesRecordMapper;
    @Autowired
    LedgerPermissionMapper ledgerPermissionMapper;

    @Override
    public Map<String, Object> getPagedExpenseRecord(Integer userId, Integer ledgerId, String year, String month, String date, Boolean type, Integer pageNum, Integer pageSize) {
        System.out.println("Do getPagedExpenseRecord()");
        //查询开始的时间
        Date startTime = null;
        //查询结束的时间
        Date endTime = null;
        try {

            if (!date.equals("")) {//如果提供了具体日期
                startTime = TimeUtil.tranStringToDate(date);
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(startTime);
                endCalendar.add(Calendar.DAY_OF_YEAR, 1);
                endTime = endCalendar.getTime();
            } else if (!month.equals("")) {//如果只提供了某年某月，转换时间为某个月起止
                startTime = TimeUtil.tranStringToDate(month + "-01");
                endTime = TimeUtil.tranStringToDate(month + "-01");
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(endTime);
                endCalendar.add(Calendar.MONTH, 1);
                endTime = endCalendar.getTime();
            } else if (!year.equals("")) {//如果只提供了年份，转换时间为某年起止。
                startTime = TimeUtil.tranStringToDate(year + "-01-01");
                endTime = TimeUtil.tranStringToDate(year + "-01-01");
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(endTime);
                endCalendar.add(Calendar.YEAR, 1);
                endTime = endCalendar.getTime();
            }
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        System.out.println("开始日期：" + startTime);
        System.out.println("结束日期：" + endTime);
        System.out.println("用户ID：" + userId);
        System.out.println("账本ID：" + ledgerId);
        //校验是否具有操作当前账本的权限
        LedgerPermission ledgerPermission = ledgerPermissionMapper.getOne(userId, ledgerId);
        if (ledgerPermission == null) {
            System.out.println("NO PERMISSION");
            return null;
        }
        System.out.println(ledgerPermission);
        System.out.println("pageNum:" + pageNum);
        System.out.println("pageSize:" + pageSize);
        int offset = (pageNum - 1) * pageSize;
        RowBounds rowBounds = new RowBounds(offset, pageSize);
        System.out.println(rowBounds);
        System.out.println("userId:" + userId);
        System.out.println("ledgerId:" + ledgerId);
        System.out.println("type:" + type);
        List<ExpensesRecordDto> list = null;
        //按照账本id查询记录
        if (type == null) {
            list = expensesRecordMapper.getPagedByLedgerIdAndTimeZone(rowBounds, userId, ledgerId, startTime, endTime);
        } else {
            list = expensesRecordMapper.getPagedByLedgerIdAndTimeZoneAndType(rowBounds, userId, ledgerId, type, startTime, endTime);
        }

        System.out.println(list.size());
        System.out.println(list);
        Map<String, Object> res = new HashMap<>(4);
        res.put("pageNumber", offset);
        res.put("pageSize", pageSize);
        res.put("total", list.size());
        res.put("result", list);
        System.out.println(res);
        return res;

    }

    @Override
    public Integer insert(int userId, int ledgerId, Double value, boolean type, String kind, String remark, Date dateRaw) {
        System.out.println("Do insert");
        //校验是否具有操作当前账本的权限
        System.out.println("UserId:" + userId);
        System.out.println("LedgerId:" + ledgerId);
        LedgerPermission ledgerPermission = ledgerPermissionMapper.getOne(userId, ledgerId);
        if (ledgerPermission == null) {
            System.out.println("NO PERMISSION!");
            return -ResponseCode.UN_AUTH.getCode();
        }
        System.out.println(ledgerPermission);
        Integer insertRes = expensesRecordMapper.insert(userId, ledgerId, value, type, kind, remark, dateRaw);
        if (insertRes != 0) {
            System.out.println(insertRes);
            return insertRes;
        }
        return -ResponseCode.SERVER_ERROR.getCode();
    }

    /**
     * 判断用户对该条消费记录是否有操作权限
     *
     * @param id     消费记录id
     * @param userId 用户id
     * @return true - 没有操作权限
     */
    public boolean hasExpensesRecordPermission(Integer id, Integer userId) {
        Long cnt = expensesRecordMapper.selectCount(id, userId);
        return cnt == 0;
    }

    @Override
    public Boolean delete(Integer id, Integer userId) {
        System.out.println("Do delete");
        System.out.println("id:" + id + " userId:" + userId);
        if (hasExpensesRecordPermission(id, userId)) {
            return false;
        }
        return expensesRecordMapper.delete(id);
    }

    @Override
    public Boolean update(ExpensesRecordDto expensesRecordDto) {
        System.out.println("Do update");
        System.out.println(expensesRecordDto);
        Integer id = expensesRecordDto.getId();
        Integer userId = expensesRecordDto.getUserId();
        Double value = expensesRecordDto.getValue();
        Boolean type = expensesRecordDto.getType();
        String kind = expensesRecordDto.getKind();
        Date date = new Date(70, 0, 1);
        try {
            date = TimeUtil.tranStringToDate(expensesRecordDto.getDate());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String remark = expensesRecordDto.getRemark();
        //检查是否有更新记录的权限
        if (hasExpensesRecordPermission(id, userId)) {
            return false;
        }
        int effectedRow = expensesRecordMapper.update(id, value, type, kind, date, remark);
        return effectedRow != 0;
    }

    /**
     * 精确查询kind与模糊查询remark的结果。
     * 全账本搜索当前用户的相关记录
     *
     * @param kind   种类
     * @param remark 备注
     * @return
     */
    @Override
    public Map<String, Object> getSelecetdExpensesRecord(int userId, String kind, String remark, Integer ledgerId, String year, String month, String date, Boolean type, Integer pageNum, Integer pageSize) {
        //查询开始的时间
        Date startTime = null;
        //查询结束的时间
        Date endTime = null;
        try {
            if (!date.equals("")) {//如果提供了具体日期
                startTime = TimeUtil.tranStringToDate(date);
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(startTime);
                endCalendar.add(Calendar.DAY_OF_YEAR, 1);
                endTime = endCalendar.getTime();
            } else if (!month.equals("")) {//如果只提供了某年某月，转换时间为某个月起止
                startTime = TimeUtil.tranStringToDate(month + "-01");
                endTime = TimeUtil.tranStringToDate(month + "-01");
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(endTime);
                endCalendar.add(Calendar.MONTH, 1);
                endTime = endCalendar.getTime();
            } else if (!year.equals("")) {//如果只提供了年份，转换时间为某年起止。
                startTime = TimeUtil.tranStringToDate(year + "-01-01");
                endTime = TimeUtil.tranStringToDate(year + "-01-01");
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(endTime);
                endCalendar.add(Calendar.YEAR, 1);
                endTime = endCalendar.getTime();
            }
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        System.out.println("开始日期：" + startTime);
        System.out.println("结束日期：" + endTime);
        System.out.println("用户ID：" + userId);
        System.out.println("账本ID：" + ledgerId);
        //校验是否具有操作当前账本的权限
        LedgerPermission ledgerPermission = ledgerPermissionMapper.getOne(userId, ledgerId);
        if (ledgerPermission == null) {
            System.out.println("NO PERMISSION");
            return null;
        }
        System.out.println(ledgerPermission);
        System.out.println("pageNum:" + pageNum);
        System.out.println("pageSize:" + pageSize);
        int offset = (pageNum - 1) * pageSize;
        RowBounds rowBounds = new RowBounds(offset, pageSize);
        System.out.println(rowBounds);
        System.out.println("userId:" + userId);
        System.out.println("ledgerId:" + ledgerId);
        System.out.println("type:" + type);
        List<ExpensesRecordDto> list = null;
        //按照userId进行查询
        if (type == null) {
            list = expensesRecordMapper.getPagedByLedgerIdAndTimeZone(rowBounds, userId, ledgerId, startTime, endTime);
        } else {
            list = expensesRecordMapper.getPagedByLedgerIdAndTimeZoneAndType(rowBounds, userId, ledgerId, type, startTime, endTime);
        }
        System.out.println(list.size());
        System.out.println(list);
        System.out.println("----");
        System.out.println("kind:" + kind);
        System.out.println("remark:" + remark);
        Iterator<ExpensesRecordDto> iterator = list.iterator();
        while (iterator.hasNext()) {
            ExpensesRecordDto e = iterator.next();
            System.out.println("==> " + e.getKind());
            System.out.println("==> " + e.getRemark());
            if (!(kind.equals(e.getKind()) || e.getRemark().contains(remark))) {
                iterator.remove();
            }
        }
        System.out.println("----");
        System.out.println(list.size());
        System.out.println(list);
        Map<String, Object> res = new HashMap<>(4);
        res.put("pageNumber", offset);
        res.put("pageSize", pageSize);
        res.put("total", list.size());
        res.put("result", list);
        System.out.println(res);
        return res;
    }

}
