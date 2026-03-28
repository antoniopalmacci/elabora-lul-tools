package it.ecubit.elabora.lul.tools.model;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Employee implements Serializable {

	private static final long serialVersionUID = -2047517343340238179L;

	private String id;
	private String name;
	private String surname;
	private String address;
	private String cap;
	private String fiscalCode;
	private Company company;

}
