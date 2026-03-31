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
	private String headquarters;
	private String employeeCode; // 0000009
	private String level; // IMP A1
	private String birthDate; // 09-12-1968
	private String hireDate; // 01-12-2020
	private String terminationDate; // opzionale
}
