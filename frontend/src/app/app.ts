import { Component } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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
  error: string | null = null;
  loading = false;

  constructor(private http: HttpClient) {}

  calculate() {
    this.error = null;
    this.loading = true;
    this.result = null;

    const params = new HttpParams()
      .set('principal', this.amount)
      .set('years', this.years);

    this.http.get<number>(`http://localhost:8080/funds/future-value/${this.selectedFund}`, { params }).subscribe({
      next: (value) => {
        this.result = value;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Could not reach the backend. Is the Spring server running on port 8080?';
        this.loading = false;
        console.error(err);
      }
    });
  }
}