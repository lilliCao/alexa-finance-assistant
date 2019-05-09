package model.banking;


import org.springframework.hateoas.ResourceSupport;

import java.util.Date;

public class Card extends ResourceSupport {

	private Number cardId;
	private String accountNumber;
	private String cardNumber;
	private CardType cardType;
	private Status status;
	private String expirationDate;

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public Number getCardId() {
		return cardId;
	}

	public void setCardId(Number cardId) {
		this.cardId = cardId;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public CardType getCardType() {
		return cardType;
	}

	public void setCardType(CardType cardType) {
		this.cardType = cardType;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public enum CardType {
		DEBIT,
		CREDIT
	}

	public enum Status {
		ACTIVE,
		INACTIVE,
		BLOCKED,
		EXPIRED
	}

}
