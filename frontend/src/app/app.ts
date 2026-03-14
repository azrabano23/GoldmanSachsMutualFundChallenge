import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  selectedFund = 'VSMPX';
  amount = 0;
  years = 0;
  result: number | null = null;

  calculate() {
    // placeholder calculation until Rayan's backend is ready
    // FV = P * e^(rt) with a hardcoded rate of 0.1 for now
    const r = 0.1;
    this.result = this.amount * Math.pow(Math.E, r * this.years);
  }
}