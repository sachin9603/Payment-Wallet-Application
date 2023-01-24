package com.masai.service;

import com.masai.dao.CustomerDAO;
import com.masai.dao.SessionDAO;
import com.masai.dao.TransactionDAO;
import com.masai.dao.WalletDAO;
import com.masai.exception.BankAccountException;
import com.masai.exception.CustomerException;
import com.masai.exception.LoginException;
import com.masai.exception.TransactionException;
import com.masai.model.CurrentUserSession;
import com.masai.model.Customer;
import com.masai.model.Transaction;
import com.masai.model.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WalletServiceImpl implements WalletService {

    /*
    * Wallet Service
1.	getCustomer -> wallet to isLogin to currentUserSession(mobile) to be matched with customer mobile
2.	depositAmount -> currentUserSession(mobile) to be matched with customer mobile to add balance to wallet
3.	fundTransfer -> params(toMobile,DestMobile,amount) check login, srcMob===currentSession(call getCustomer())
*    && check destMobile is exists if exists call depositAmount() else throw BankAccountNotFoundexception
4.	showBalance

    * */

    @Autowired
    private WalletDAO walletDAO;

    @Autowired
    private CustomerDAO customerDAO;

    @Autowired
    private SessionDAO sessionDAO;

    @Autowired
    private TransactionDAO transactionDAO;

    @Autowired
    private TransactionServiceImpl transactionService;

    public CurrentUserSession isLogin(String key) throws LoginException {
        CurrentUserSession currentUserSession = sessionDAO.findByUuid(key);
        if (currentUserSession != null)
            return currentUserSession;
        else
            return null;
    }
    @Override
    public double showBalance(String key) throws CustomerException, LoginException {
        if (isLogin(key) != null) {
            CurrentUserSession currentUserSession = sessionDAO.findByUuid(key);
            Customer customer = customerDAO.findByMobileNumber(currentUserSession.getUserId());
            if (customer != null)
                return customer.getWallet().getBalance();
            else
                throw new CustomerException("No Account found ... ");
        } else {
            throw new LoginException("You are not logged in ...");
        }
    }

    @Override
    public Customer getCustomers(String key) throws CustomerException, LoginException {
        CurrentUserSession aao = isLogin(key);
        if (aao != null) {
            return customerDAO.findByMobileNumber(aao.getUserId());
        } else {
            throw new LoginException("You are not logged in ...");
        }
    }
    @Override
    public String depositAmount(String key, Double amount) throws CustomerException, LoginException {

        CurrentUserSession aao = isLogin(key);
        if (aao != null) {
            Customer customer = customerDAO.findByMobileNumber(aao.getUserId());
            customer.getWallet().setBalance(customer.getWallet().getBalance() + amount);
            customerDAO.save(customer);
            walletDAO.save(customer.getWallet());
            return "Transaction successful..";
        } else {
            throw new LoginException("You are not logged in ...");
        }
    }

    @Override
    public Customer fundTransfer(String desMob, Double amount,String key) throws CustomerException, LoginException, BankAccountException, TransactionException {
        CurrentUserSession aao = isLogin(key);
        if (aao != null) {
            Customer customer = customerDAO.findByMobileNumber(aao.getUserId());
            Double balance = customer.getWallet().getBalance();
            if (balance >= amount) {

                customerDAO.save(customer);
                walletDAO.save(customer.getWallet());
                Customer desCustomer = customerDAO.findByMobileNumber(desMob);
                if (desCustomer != null) {
                    customer.getWallet().setBalance(customer.getWallet().getBalance() - amount);
                    Transaction transaction = new Transaction();
                    transaction.setWallet(customer.getWallet());
                    transaction.setAmount(amount);
                    transaction.setTransactionDate(LocalDate.now());
                    transaction.setTransactionType("Debited");
                    transaction.setDescription(desMob);
                    customer.getWallet().getTransactions().add(transaction);
                    customerDAO.save(customer);
                    walletDAO.save(customer.getWallet());

                    //desc customer
                    desCustomer.getWallet().setBalance(desCustomer.getWallet().getBalance() + amount);
                    Transaction transactiondes = new Transaction();
                    transactiondes.setWallet(desCustomer.getWallet());
                    transactiondes.setAmount(amount);
                    transactiondes.setTransactionDate(LocalDate.now());
                    transactiondes.setTransactionType("Credited");
                    transactiondes.setDescription(customer.getMobileNumber());
                    desCustomer.getWallet().getTransactions().add(transactiondes);
                    customerDAO.save(desCustomer);
                    walletDAO.save(desCustomer.getWallet());

//                    creditedFund(desMob, amount);
                    return customer;
                } else {
                    throw new CustomerException("customer not found by userId..." + desMob);
                }
            } else {
                throw new BankAccountException("Insufficient balance...");
            }
        } else {
            throw new LoginException("You are not logged in ...");
        }
    }

    @Override
    public Customer creditedFund(String desMob, Double amount) throws CustomerException, LoginException, BankAccountException, TransactionException {
        Customer customer = customerDAO.findByMobileNumber(desMob);
        if(customer!=null) {
            customer.getWallet().setBalance(customer.getWallet().getBalance()+amount);
            Transaction transaction = new Transaction();
            transaction.setWallet(customer.getWallet());
            transaction.setAmount(amount);
            transaction.setTransactionDate(LocalDate.now());
            transaction.setTransactionType("Credited");
            customer.getWallet().getTransactions().add(transaction);
            customerDAO.save(customer);
            return customer;
        } else {
            throw new CustomerException("Customer not exits with mobile number : " + desMob);
        }
    }

}
